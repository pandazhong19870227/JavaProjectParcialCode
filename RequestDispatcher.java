package cc.gamemania.modules;

import cc.gamemania.constants.Constants;
import cc.gamemania.http.api.proto.*;
import cc.gamemania.http.api.proto.SportRefundRequest;
import cc.gamemania.http.api.proto.SportRefundResponse;
import cc.gamemania.http.api.proto.SportWinRequest;
import cc.gamemania.http.api.proto.SportWinResponse;
import cc.gamemania.http.api.proto.entities.*;
import cc.gamemania.modules.account.ExternalInterface;
import cc.gamemania.modules.account.proto.*;
import cc.gamemania.modules.cashout.CashOutFacade;
import cc.gamemania.modules.config.CommandLineArgs;
import cc.gamemania.modules.game.DBTaskManager;
import cc.gamemania.modules.game.GameFacade;
import cc.gamemania.modules.game.LiveGameFacade;
import cc.gamemania.modules.game.api.proto.entities.GameEntity;
import cc.gamemania.modules.game.api.proto.entities.GameMarketOutcome;
import cc.gamemania.modules.monitor.MonitorFacade;
import cc.gamemania.modules.push.PushFacade;
import cc.gamemania.modules.push.PushMessageBodySport;
import cc.gamemania.modules.results.CompetitionWithResults;
import cc.gamemania.modules.results.ResultsManager;
import cc.gamemania.modules.season.SeasonFacade;
import cc.gamemania.modules.ticket.api.proto.entities.BetSettlementRequest;
import cc.gamemania.modules.ticket.TicketFacade;
import cc.gamemania.modules.ticket.api.proto.entities.BettingFailedEvent;
import cc.gamemania.modules.ticket.api.proto.entities.TicketManageUnit;
import cc.gamemania.modules.tournament.MySimpleTournament;
import cc.gamemania.modules.uof.GlobalEventsListener;
import cc.gamemania.modules.uof.MessageDispatcher;
import cc.gamemania.modules.uof.OddsFeedFacade;
import cc.gamemania.utils.CodeMessage;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;

import com.sportradar.unifiedodds.sdk.MessageInterest;
import com.sportradar.unifiedodds.sdk.OddsFeed;
import com.sportradar.unifiedodds.sdk.OddsFeedSessionBuilder;
import com.sportradar.unifiedodds.sdk.ReplayOddsFeed;
import com.sportradar.unifiedodds.sdk.cfg.OddsFeedConfiguration;
import com.sportradar.unifiedodds.sdk.entities.EventStatus;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by pandazhong on 17/8/4.
 */

@Component
public class RequestDispatcher implements ApplicationListener<ContextRefreshedEvent> {
    private org.slf4j.Logger logger = LoggerFactory.getLogger("BetFacade");
    @Autowired
    private GlobalEventsListener globalEventsListener;

    @Autowired
    private MonitorFacade monitorFacade;

    @Autowired
    private TicketFacade ticketFacade;

    @Autowired
    private OddsFeedFacade oddsFeedFacade;

    @Autowired
    private ExternalInterface accountFacade;

    @Autowired
    private GameFacade gameFacade;

    @Autowired
    private LiveGameFacade liveGameFacade;

    @Autowired
    private CommandLineArgs commandLineArgs;

    @Autowired
    private PushFacade pushFacade;

    @Autowired
    private ResultsManager resultsManager;

    @Autowired
    private SeasonFacade seasonFacade;

    @Autowired
    private CashOutFacade cashOutFacade;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MessageDispatcher messageDispatcher;

    @Autowired
    private DBTaskManager dbTaskManager;

    private AtomicInteger status = new AtomicInteger();

    private HashMap<String, RequestData> requestDataHashMap = new HashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        monitorFacade.onAlert("服务 " + Constants.GIT_TAG + " 启动中...");

        logger.info("git tag {}", Constants.GIT_TAG);
        logger.info("The commandline args {}", JSON.toJSONString(commandLineArgs));

        monitorFacade.startup();
        monitorFacade.onAlert("【请求分发模块】正在初始化，参数列表：" + JSON.toJSONString(commandLineArgs));

        initMarketCategory();

        pushFacade.startup();

        long lastMs = System.currentTimeMillis();

        status.set(0);

        ScheduledExecutorService service = Executors
                .newSingleThreadScheduledExecutor();

        Runnable runnable = new Runnable() {
            public void run() {
                if (status.get() != 1) {
                    monitorFacade.onAlert("【请求分发模块-监控线程】正在初始化，已经耗时【" + (int) ((System.currentTimeMillis() - lastMs) / 1000) + "】秒");
                } else {
                    service.shutdown();
                    monitorFacade.onAlert("【请求分发模块-监控线程】初始化监控线程已经退出。");
                }
            }
        };

        service.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);

        Constants.TIMESTAMP_SERVER_STARTED = System.currentTimeMillis();

        accountFacade.startup(commandLineArgs.getAccountBaseUrl(), commandLineArgs.getAccountGatewayBaseUrl(), commandLineArgs.getGrantGoldGatewayBaseUrl(), commandLineArgs.getBetSettlementGatewayBaseUrl());

        try {
            List<Locale> locales = new ArrayList<>();
            Constants.locale = new Locale(commandLineArgs.getLocaleLanguage(), commandLineArgs.getLocaleCountry());
            locales.add(Constants.locale);
            OddsFeedConfiguration config = OddsFeed.getConfigurationBuilder().setAccessToken(commandLineArgs.getAccessToken()).addDesiredLocales(locales).build();

            OddsFeed session;
            if (commandLineArgs.isEnabledReplay()) {
                session = new ReplayOddsFeed(globalEventsListener, config);
                Constants.setAppEnabled(true);
                commandLineArgs.setMtsEnabled(false);
                logger.info("Replay Mode");
            } else {
                session = new OddsFeed(globalEventsListener, config);
                logger.info("Normal Mode");
            }

            OddsFeedSessionBuilder sessionBuilder = session.getSessionBuilder();

            if (commandLineArgs.isEnabledReplay()) {
                sessionBuilder.setListener(oddsFeedFacade).setMessageInterest(MessageInterest.AllMessages).build();
                logger.info("Replay Mode Interest All Messages");
            } else {
                sessionBuilder.setListener(oddsFeedFacade).setMessageInterest(MessageInterest.PrematchMessagesOnly).build();
                sessionBuilder.setListener(oddsFeedFacade).setMessageInterest(MessageInterest.LiveMessagesOnly).build();
                logger.info("Normal Mode Interest Prematch And Live Messages");
                long ts1 = session.getProducerManager().getProducer(3).getTimestampForRecovery();
                if (ts1 <= 0) {
                    // 3 hours ago
                    ts1 = System.currentTimeMillis() - (long) (commandLineArgs.getRecoveryFromHourAgo()) * 3600 * 1000;
                }

                session.getProducerManager().setProducerRecoveryFromTimestamp(3, ts1);

                long ts2 = session.getProducerManager().getProducer(1).getTimestampForRecovery();
                if (ts2 <= 0) {
                    // 3 hours ago
                    ts2 = System.currentTimeMillis() - (long) (commandLineArgs.getRecoveryFromHourAgo()) * 3600 * 1000;
                }

                session.getProducerManager().setProducerRecoveryFromTimestamp(1, ts2);
            }

            session.open();

            if (commandLineArgs.isEnabledReplay()) {
                ((ReplayOddsFeed) session).getReplayManager().play();
            }

            oddsFeedFacade.startup(session);

            gameFacade.startup(session);
            ticketFacade.startup();

            resultsManager.startup(session);

            if (commandLineArgs.isMtsEnabled()) {
                //mtsFacade.startup(ticketFacade);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        status.set(1);

        checkMessageQueueLoop();

        checkRequestQueueLoop();

        monitorFacade.onAlert("【请求分发模块】初始化完成，总共耗时【" + (int) ((System.currentTimeMillis() - lastMs) / 1000) + "】秒");
    }

    private void checkMessageQueueLoop() {
        Runnable runnable = new Runnable() {
            public void run() {
                ticketFacade.debug();

                dbTaskManager.debug();

                messageDispatcher.debug();

                pushFacade.debug();
            }
        };

        ScheduledExecutorService service = Executors
                .newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, commandLineArgs.getCheckMessageQueueInitialDelaySecond(), commandLineArgs.getCheckMessageQueuePeriodSecond(), TimeUnit.SECONDS);
    }

    private void checkRequestQueueLoop() {
        Runnable runnable = new Runnable() {
            public void run() {
                long nowMs = System.currentTimeMillis();
                HashMap<String, RequestData> tmp = null;

                synchronized (requestDataHashMap) {
                    tmp = (HashMap<String, RequestData>) requestDataHashMap.clone();
                }

                Iterator itr = tmp.entrySet().iterator();
                while (itr.hasNext()) {
                    Map.Entry entry = (Map.Entry) itr.next();
                    String requestId = (String) entry.getKey();
                    RequestData requestData = (RequestData) entry.getValue();
                    if (nowMs - requestData.getTsBeginMs() > commandLineArgs.getRequestTimeoutMs()) {
                        synchronized (requestDataHashMap) {
                            requestDataHashMap.remove(entry.getKey());
                        }
                        String msg = JSON.toJSONString(requestData);
                        logger.info("Request takes too long to process, {}", msg);
                        monitorFacade.onAlert("【请求分发模块】请求【" + requestId + "】处理超时，详情：" + msg);
                    }
                }
            }
        };

        ScheduledExecutorService service = Executors
                .newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, commandLineArgs.getCheckRequestQueueInitialDelaySecond(), commandLineArgs.getCheckRequestQueuePeriodSecond(), TimeUnit.SECONDS);
    }

    public void addRequest(RequestData requestData) {
        synchronized (requestDataHashMap) {
            requestDataHashMap.put(requestData.getRequestId(), requestData);
        }
    }

    public void removeRequest(RequestData requestData) {
        synchronized (requestDataHashMap) {
            requestDataHashMap.remove(requestData.getRequestId());
        }
    }

    /**
     * 获取指定比赛所有market的赔率
     *
     * @param request
     * @return
     */
    public MatchMarketsResponse matchMarkets(MatchMarketsRequest request) {
        MatchMarketsResponse response = new MatchMarketsResponse();

        if (request != null) {
            /*
            if(!accountFacade.isLogin(request.getAuthToken())){
                response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                response.setMsg(Constants.ERROR_AUTH_FAILED);
                return response;
            }
            */

            List<MarketLine> data = gameFacade.getMarketLines(request.getMatchId());

            if (data != null) {
                for (MarketLine marketLine : data){
                    marketLine.setCategory(getCategory(marketLine.getMarketId()));
                }
                response.setCode(Constants.CODE_SUCCESSFUL);
                response.setData(data);
            } else {
                response.setCode(Constants.CODE_NOT_FOUND_RESOURCE);
                response.setMsg(Constants.ERROR_NOT_FOUND_ODDS);
            }
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * LIVE 获取指定比赛所有market的赔率
     *
     * @param request
     * @return
     */
    public MatchMarketsResponse liveMatchMarkets(MatchMarketsRequest request) {
        MatchMarketsResponse response = new MatchMarketsResponse();

        if (!commandLineArgs.isEnabledLive()) {
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_NOT_SUPPORT_LIVE_BETTING);
            return response;
        }

        if (request != null) {
            List<MarketLine> data = liveGameFacade.getMarketLines(request.getMatchId());
            if (data != null) {
                for (MarketLine marketLine : data){
                    marketLine.setCategory(getCategory(marketLine.getMarketId()));
                }
                response.setCode(Constants.CODE_SUCCESSFUL);
                response.setData(data);
            } else {
                response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                response.setMsg(Constants.ERROR_NOT_FOUND_ODDS);
            }
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * 获取一组运动
     *
     * @param request
     * @return
     */
    public SportCategoriesResponse sportCategories(SportCategoriesRequest request) {
        SportCategoriesResponse response = new SportCategoriesResponse();

        if (request != null) {
            /*
            if(!accountFacade.isLogin(request.getAuthToken())){
                response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                response.setMsg(Constants.ERROR_AUTH_FAILED);
                return response;
            }
            */

            List<SportItem> data = gameFacade.getSportItems();

            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * 获取一组比赛以及默认赔率
     *
     * @param request
     * @return
     */
    public MatchesOddsResponse matchesOdds(MatchesOddsRequest request) {
        MatchesOddsResponse response = new MatchesOddsResponse();

        if (request != null) {
            List<GameMarketLine> data = gameFacade.getMatchesOdds(request.getSportId(), request.getClassId(), request.getStartGameId(), request.getTimeBegin(), request.getNumMax(), request.getTimeEnd(), request.getCountryName(), request.getTournamentId(), request.getMarketId());
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * LIVE 获取一组比赛以及默认赔率
     *
     * @param request
     * @return
     */
    public MatchesOddsResponse liveMatchesOdds(MatchesOddsRequest request) {
        MatchesOddsResponse response = new MatchesOddsResponse();

        if (!commandLineArgs.isEnabledLive()) {
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_NOT_SUPPORT_LIVE_BETTING);
            return response;
        }

        if (request != null) {
            List<GameMarketLine> data = liveGameFacade.getMatchesOdds(request.getSportId(), request.getClassId(), request.getStartGameId(), request.getTimeBegin(), request.getNumMax(), request.getTimeEnd(), request.getCountryName(), request.getTournamentId(), request.getMarketId());
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * 获取与指定类型的运动相关的国家
     *
     * @param request
     * @return
     */
    public SportCountriesResponse sportCountries(SportCountriesRequest request) {
        SportCountriesResponse response = new SportCountriesResponse();

        if (request != null) {
            List<CountryItem> data = gameFacade.getSportCountries(request.getSportId());
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }


        return response;
    }

    /**
     * 获取与指定类型的运动相关的国家 v2
     *
     * @param request
     * @return
     */
    public SportCountriesResponse sportCountriesV2(SportCountriesRequest request) {
        SportCountriesResponse response = new SportCountriesResponse();

        if (request != null) {
            List<CountryItem> data = gameFacade.getSportCountries();
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }


        return response;
    }

    /**
     * LIVE 获取与指定类型的运动相关的国家
     *
     * @param request
     * @return
     */
    public SportCountriesResponse liveSportCountries(SportCountriesRequest request) {
        SportCountriesResponse response = new SportCountriesResponse();

        if (!commandLineArgs.isEnabledLive()) {
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_NOT_SUPPORT_LIVE_BETTING);
            return response;
        }

        if (request != null) {
            List<CountryItem> data = liveGameFacade.getSportCountries(request.getSportId());
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }


        return response;
    }

    /**
     * LIVE 获取与指定类型的运动相关的国家 v2
     *
     * @param request
     * @return
     */
    public SportCountriesResponse liveSportCountriesV2(SportCountriesRequest request) {
        SportCountriesResponse response = new SportCountriesResponse();

        if (!commandLineArgs.isEnabledLive()) {
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_NOT_SUPPORT_LIVE_BETTING);
            return response;
        }

        if (request != null) {
            List<CountryItem> data = liveGameFacade.getSportCountries();
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }


        return response;
    }

    /**
     * 获取一组联赛
     *
     * @param request
     * @return
     */
    public TournamentsResponse tournaments(TournamentsRequest request) {
        TournamentsResponse response = new TournamentsResponse();

        if (request != null) {
            List<CountryTournamentItem> data = gameFacade.getSportTournaments(request.getSportId(), request.getCountryName(), request.getTimestampStartMin(), request.getTimestampStartMax());
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * LIVE 获取一组联赛
     *
     * @param request
     * @return
     */
    public TournamentsResponse liveTournaments(TournamentsRequest request) {
        TournamentsResponse response = new TournamentsResponse();

        if (!commandLineArgs.isEnabledLive()) {
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_NOT_SUPPORT_LIVE_BETTING);
            return response;
        }

        if (request != null) {
            List<CountryTournamentItem> data = liveGameFacade.getSportTournaments(request.getSportId(), request.getCountryName());
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * 获取热门比赛
     *
     * @param request
     * @return
     */
    public HotMatchesResponse hotMatches(HotMatchesRequest request) {
        HotMatchesResponse response = new HotMatchesResponse();

        if (request != null) {
            List<GameMarketLine> data = gameFacade.hotMatches();
            response.setCode(Constants.CODE_SUCCESSFUL);
            response.setData(data);
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * 获取比赛详情
     *
     * @param request
     * @return
     */
    public MatchInfoResponse matchInfo(MatchInfoRequest request) {
        MatchInfoResponse response = new MatchInfoResponse();

        if (request != null) {
            GameMarketLine data = gameFacade.matchInfo(request.getGameId());
            if (data != null) {
                response.setCode(Constants.CODE_SUCCESSFUL);
                response.setData(data);
            } else {
                response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                response.setMsg(Constants.ERROR_NOT_FOUND_ODDS);
            }
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    /**
     * 获取比赛详情
     *
     * @param request
     * @return
     */
    public MatchInfoResponse liveMatchInfo(MatchInfoRequest request) {
        MatchInfoResponse response = new MatchInfoResponse();

        if (request != null) {
            GameMarketLine data = liveGameFacade.matchInfo(request.getGameId());
            if (data != null) {
                response.setCode(Constants.CODE_SUCCESSFUL);
                response.setData(data);
            } else {
                response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                response.setMsg(Constants.ERROR_NOT_FOUND_ODDS);
            }
        } else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
        }

        return response;
    }

    private boolean isSeasonBet(BaseBetRequest request){
        if (request.getSelections() != null && request.getSelections().size() > 0){
            for (BetSelection selection : request.getSelections()){
                if (selection != null && selection.getGameId() != null && selection.getGameId().contains("season")){
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Single下注
     *
     * @param request
     * @return
     */
    public SingleBetResponse singleBet(SingleBetRequest request) {
        SingleBetResponse response = new SingleBetResponse();

        if (isSeasonBet(request)){
            return singleBetSeason(request);
        }

        /**
        if (!Constants.isAppEnabled()) {
            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_SERVICE_IS_NOT_AVAILABLE);
            event.setMessage(BettingFailedEvent.MESSAGE_SERVICE_IS_NOT_AVAILABLE);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_SERVICE_IS_NOT_AVAILABLE);
            return response;
        }**/

        if (request == null || request.getSelections() == null || request.getSelections().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (request.getAmount() < commandLineArgs.getSingleBetLeastAmount()) {
            logger.error("singleBet failed: bad amount {} not reached least requirement {}", request.getAmount(), commandLineArgs.getSingleBetLeastAmount());
            response.setCode(Constants.CODE_AMOUNT_LIMITED);
            response.setMsg("Minimum stake should be " + commandLineArgs.getSingleBetLeastAmount() + "ksh for each selection");
            return response;
        }

        for (BetSelection betSelection : request.getSelections()) {
            if (betSelection == null) {
                logger.error("singleBet failed: at least one selection is null");
                monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：至少一个selection为空");
                response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
                response.setMsg(Constants.ERROR_BAD_REQUEST);
                return response;
            }
        }

        PushMessageBodySport message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET, request);
        if (message != null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_MATCH_OR_MARKET_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_MATCH_OR_MARKET_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            logger.error("singleBet failed: at least on game or market not found");
            monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：至少一个比赛或者Market找不到");
            response.setCode(Constants.CODE_BET_MESSAGE);
            BetResponseData data = new BetResponseData();
            data.setMessage(message);
            response.setData(data);
            return response;
        }

        String sportId = null;

        long nowMs = System.currentTimeMillis();
        for (BetSelection betSelection : request.getSelections()) {
            if (sportId == null) {
                sportId = betSelection.getSportId();
            } else {
                if (!sportId.equals(betSelection.getSportId())) {
                    monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：同一个下注请求中包含了不同的运动类型。详情：用户令牌=" + request.getAuthToken());
                    response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                    response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
                    return response;
                }
            }

            GameEntity gameEntity = gameFacade.getGame(betSelection.getGameId());
            if (gameEntity == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：找不到比赛。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            GameMarketOutcome outcome = gameEntity.getOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId());
            if (outcome == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：Market未激活或者找不到下注选项。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            if (gameEntity.getTimestampStart() - nowMs < commandLineArgs.getPrematchBetTimeWindowMinute() * 60000){
                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_TIME_LIMIT);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_TIME_LIMIT);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                response.setCode(Constants.CODE_APP_DISABLED);
                response.setMsg(Constants.ERROR_BET_TIME_LIMIT);
                return response;
            }

            betSelection.setHomeTeamName(gameEntity.getFullHomeTeamName());
            betSelection.setAwayTeamName(gameEntity.getFullAwayTeamName());
            betSelection.setOdd(outcome.getOdd());
            betSelection.setTournamentId(gameEntity.getTournamentId());
            betSelection.setTournamentName(gameEntity.getTournamentName());
            betSelection.setTimestampMsStart(gameEntity.getTimestampStart());
            //betSelection.setShortGameId(Helper.getShortId(betSelection.getGameId()) + "");
            betSelection.setMarketDescription(gameFacade.getMarketDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers()));
            betSelection.setOutcomeDescription(gameFacade.getOutcomeDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId()));
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("singleBet: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();
        request.setUid(uid);

        request.setBetType(BaseBetRequest.BET_TYPE_SINGLE);

        try {
            SportBetResponse resp = ticketFacade.onIncomingSingleBet(TicketManageUnit.PRE_MATCH_BET, request, response);
            setBetResponse(response, resp);
        } catch (Exception e) {
            logger.error("Exception: {}", JSON.toJSONString(e));
            monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：" + e.getMessage() + "。详情：用户令牌=" + request.getAuthToken());
            CodeMessage cm = CodeMessage.parse(e.getMessage());
            response.setCode(cm.getCode());
            response.setMsg(cm.getMsg());
            if (response.getData() != null) {
                response.getData().setMaybeWin(0);
            }
        }

        return response;
    }

    /**
     * Single Season下注
     *
     * @param request
     * @return
     */
    public SingleBetResponse singleBetSeason(SingleBetRequest request) {
        SingleBetResponse response = new SingleBetResponse();

        if (!Constants.isAppEnabled()) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_SERVICE_IS_NOT_AVAILABLE);
            event.setMessage(BettingFailedEvent.MESSAGE_SERVICE_IS_NOT_AVAILABLE);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_SERVICE_IS_NOT_AVAILABLE);
            return response;
        }

        if (request == null || request.getSelections() == null || request.getSelections().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (request.getAmount() < commandLineArgs.getSingleBetLeastAmount()) {
            logger.error("singleBetSeason failed: bad amount {} not reached least requirement {}", request.getAmount(), commandLineArgs.getSingleBetLeastAmount());
            response.setCode(Constants.CODE_AMOUNT_LIMITED);
            response.setMsg("Minimum stake should be " + commandLineArgs.getSingleBetLeastAmount() + "ksh for each selection");
            return response;
        }

        for (BetSelection betSelection : request.getSelections()) {
            if (betSelection == null) {
                logger.error("singleBetSeason failed: at least one selection is null");
                monitorFacade.onAlert("【请求分发模块】singleBetSeason下注请求失败，原因：至少一个selection为空");
                response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
                response.setMsg(Constants.ERROR_BAD_REQUEST);
                return response;
            }
        }

        PushMessageBodySport message = checkBetRequestSeason(PushMessageBodySport.MESSAGE_TYPE_MARKET_SEASON, request);
        if (message != null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_MATCH_OR_MARKET_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_MATCH_OR_MARKET_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            logger.error("singleBetSeason failed: at least on game or market not found");
            monitorFacade.onAlert("【请求分发模块】singleBetSeason下注请求失败，原因：至少一个比赛或者Market找不到");
            response.setCode(Constants.CODE_BET_MESSAGE);
            BetResponseData data = new BetResponseData();
            data.setMessage(message);
            response.setData(data);
            return response;
        }

        String sportId = null;

        for (BetSelection betSelection : request.getSelections()) {
            if (sportId == null) {
                sportId = betSelection.getSportId();
            } else {
                if (!sportId.equals(betSelection.getSportId())) {
                    monitorFacade.onAlert("【请求分发模块】singleBetSeason下注请求失败，原因：同一个下注请求中包含了不同的运动类型。详情：用户令牌=" + request.getAuthToken());
                    response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                    response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
                    return response;
                }
            }

            GameEntity gameEntity = seasonFacade.getSeason(betSelection.getGameId());
            if (gameEntity == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】singleBetSeason下注请求失败，原因：找不到比赛。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_SEASON, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            GameMarketOutcome outcome = gameEntity.getOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId());
            if (outcome == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】singleBetSeason下注请求失败，原因：Market未激活或者找不到下注选项。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_SEASON, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            betSelection.setHomeTeamName(gameEntity.getFullHomeTeamName());
            betSelection.setAwayTeamName(gameEntity.getFullAwayTeamName());
            betSelection.setOdd(outcome.getOdd());
            betSelection.setTournamentId(gameEntity.getTournamentId());
            betSelection.setTournamentName(gameEntity.getTournamentName());
            betSelection.setTimestampMsStart(gameEntity.getTimestampStart());
            betSelection.setMarketDescription(seasonFacade.getMarketDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers()));
            betSelection.setOutcomeDescription(seasonFacade.getOutcomeDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId()));
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】singleBetSeason下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】singleBetSeason下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("singleBetSeason: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();
        request.setUid(uid);

        request.setBetType(BaseBetRequest.BET_TYPE_SINGLE);

        try {
            SportBetResponse resp = ticketFacade.onIncomingSingleBet(TicketManageUnit.SEASON_BET, request, response);
            setBetResponse(response, resp);
        } catch (Exception e) {
            logger.error("singleBetSeason Exception: {}", JSON.toJSONString(e));
            monitorFacade.onAlert("【请求分发模块】singleBetSeason 下注请求失败，原因：" + e.getMessage() + "。详情：用户令牌=" + request.getAuthToken());
            CodeMessage cm = CodeMessage.parse(e.getMessage());
            response.setCode(cm.getCode());
            response.setMsg(cm.getMsg());
            if (response.getData() != null) {
                response.getData().setMaybeWin(0);
            }
        }

        return response;
    }

    /**
     * LIVE Single下注
     *
     * @param request
     * @return
     */
    public SingleBetResponse liveSingleBet(SingleBetRequest request) {
        SingleBetResponse response = new SingleBetResponse();

        if (!Constants.isAppEnabled()) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_SERVICE_IS_NOT_AVAILABLE);
            event.setMessage(BettingFailedEvent.MESSAGE_SERVICE_IS_NOT_AVAILABLE);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_SERVICE_IS_NOT_AVAILABLE);
            return response;
        }

        if (!commandLineArgs.isEnabledLive()) {
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_NOT_SUPPORT_LIVE_BETTING);
            return response;
        }

        if (request == null || request.getSelections() == null || request.getSelections().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (request.getAmount() < commandLineArgs.getSingleBetLeastAmount()) {
            logger.error("LIVE singleBet failed: bad amount {} not reached least requirement {}", request.getAmount(), commandLineArgs.getSingleBetLeastAmount());
            response.setCode(Constants.CODE_AMOUNT_LIMITED);
            response.setMsg("LIVE Minimum stake should be " + commandLineArgs.getSingleBetLeastAmount() + "ksh for each selection");
            return response;
        }

        for (BetSelection betSelection : request.getSelections()) {
            if (betSelection == null) {
                logger.error("LIVE singleBet failed: at least one selection is null");
                monitorFacade.onAlert("【请求分发模块】LIVE Single下注请求失败，原因：至少一个selection为空");
                response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
                response.setMsg(Constants.ERROR_BAD_REQUEST);
                return response;
            }
        }

        PushMessageBodySport message = liveCheckBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_LIVE, request);
        if (message != null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_MATCH_OR_MARKET_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_MATCH_OR_MARKET_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            logger.error("LIVE singleBet failed: at least on game or market not found");
            monitorFacade.onAlert("【请求分发模块】LIVE Single下注请求失败，原因：至少一个比赛或者Market找不到");
            response.setCode(Constants.CODE_BET_MESSAGE);
            BetResponseData data = new BetResponseData();
            data.setMessage(message);
            response.setData(data);
            return response;
        }

        String sportId = null;

        for (BetSelection betSelection : request.getSelections()) {
            if (sportId == null) {
                sportId = betSelection.getSportId();
            } else {
                if (!sportId.equals(betSelection.getSportId())) {
                    monitorFacade.onAlert("【请求分发模块】LIVE Single下注请求失败，原因：同一个下注请求中包含了不同的运动类型。详情：用户令牌=" + request.getAuthToken());
                    response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                    response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
                    return response;
                }
            }

            GameEntity gameEntity = liveGameFacade.getGame(betSelection.getGameId());
            if (gameEntity == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】LIVE Single下注请求失败，原因：找不到比赛。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = liveCheckBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_LIVE, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            GameMarketOutcome outcome = gameEntity.getOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId());
            if (outcome == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】LIVE Single下注请求失败，原因：Market未激活或者找不到下注选项。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = liveCheckBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_LIVE, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            betSelection.setHomeTeamName(gameEntity.getFullHomeTeamName());
            betSelection.setAwayTeamName(gameEntity.getFullAwayTeamName());
            betSelection.setOdd(outcome.getOdd());
            betSelection.setTournamentId(gameEntity.getTournamentId());
            betSelection.setTournamentName(gameEntity.getTournamentName());
            betSelection.setTimestampMsStart(gameEntity.getTimestampStart());
            //betSelection.setShortGameId(Helper.getShortId(betSelection.getGameId()) + "");
            betSelection.setMarketDescription(liveGameFacade.getMarketDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers()));
            betSelection.setOutcomeDescription(liveGameFacade.getOutcomeDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId()));
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】LIVE Single下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】LIVE Single下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("LIVE singleBet: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();
        request.setUid(uid);

        request.setBetType(BaseBetRequest.BET_TYPE_SINGLE);

        try {
            liveGameFacade.onLiveBetting(request);
            SportBetResponse resp = ticketFacade.onIncomingSingleBet(TicketManageUnit.LIVE_MATCH_BET, request, response);
            setBetResponse(response, resp);
        } catch (Exception e) {
            logger.error("Exception: {}", JSON.toJSONString(e));
            monitorFacade.onAlert("【请求分发模块】Single下注请求失败，原因：" + e.getMessage() + "。详情：用户令牌=" + request.getAuthToken());
            CodeMessage cm = CodeMessage.parse(e.getMessage());
            response.setCode(cm.getCode());
            response.setMsg(cm.getMsg());
            if (response.getData() != null) {
                response.getData().setMaybeWin(0);
            }
        }

        return response;
    }

    private boolean containsSameMatches(List<BetSelection> selections){
        if (selections == null){
            return false;
        }

        for (BetSelection s1 : selections){
            int count = 0;
            for (BetSelection s2 : selections){
                if (s1.getGameId().equals(s2.getGameId())){
                    count++;
                    if (count > 1){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Multiple下注
     *
     * @param request
     * @return
     */
    public MultipleBetResponse multipleBet(MultipleBetRequest request) {
        MultipleBetResponse response = new MultipleBetResponse();

        if (isSeasonBet(request)){
            return multipleBetSeason(request);
        }

        /**
        if (!Constants.isAppEnabled()) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_SERVICE_IS_NOT_AVAILABLE);
            event.setMessage(BettingFailedEvent.MESSAGE_SERVICE_IS_NOT_AVAILABLE);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_SERVICE_IS_NOT_AVAILABLE);
            return response;
        }*/

        if (request == null || request.getSelections() == null || request.getSelections().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (request.getAmount() < commandLineArgs.getMultipleBetLeastAmount()) {
            logger.error("multipleBet failed: bad amount {} not reached least requirement {}", request.getAmount(), commandLineArgs.getSingleBetLeastAmount());
            response.setCode(Constants.CODE_AMOUNT_LIMITED);
            response.setMsg("Minimum total stake should be " + commandLineArgs.getMultipleBetLeastAmount() + "ksh");
            return response;
        }

        for (BetSelection betSelection : request.getSelections()) {
            if (betSelection == null) {
                logger.error("multipleBet failed: at least one selection is null");
                monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：至少一个selection为空");
                response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
                response.setMsg(Constants.ERROR_BAD_REQUEST);
                return response;
            }
        }

        PushMessageBodySport message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET, request);
        if (message != null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_MATCH_OR_MARKET_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_MATCH_OR_MARKET_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            logger.error("multipleBet failed: at least on game or market not found");
            monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：至少一个比赛或者Market找不到");
            response.setCode(Constants.CODE_BET_MESSAGE);
            BetResponseData data = new BetResponseData();
            data.setMessage(message);
            response.setData(data);
            return response;
        }

        if (request.getSelections().size() < 2) {
            monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：下注数目少于2。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (containsSameMatches(request.getSelections())){
            monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：同一个下注请求中包含了俩个或者以上相同的比赛。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
            return response;
        }

        String sportId = null;

        long nowMs = System.currentTimeMillis();
        for (BetSelection betSelection : request.getSelections()) {
            if (sportId == null) {
                sportId = betSelection.getSportId();
            } else {
                if (!sportId.equals(betSelection.getSportId())) {
                    monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：同一个下注请求中包含了不同的运动类型。详情：用户令牌=" + request.getAuthToken());
                    response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                    response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
                    return response;
                }
            }

            GameEntity gameEntity = gameFacade.getGame(betSelection.getGameId());
            if (gameEntity == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);


                monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：找不到比赛。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            GameMarketOutcome outcome = gameEntity.getOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId());
            if (outcome == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);


                monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：Market未激活或者找不到下注选项。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            if (gameEntity.getTimestampStart() - nowMs < commandLineArgs.getPrematchBetTimeWindowMinute() * 60000){

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_TIME_LIMIT);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_TIME_LIMIT);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);


                response.setCode(Constants.CODE_APP_DISABLED);
                response.setMsg(Constants.ERROR_BET_TIME_LIMIT);
                return response;
            }

            betSelection.setHomeTeamName(gameEntity.getFullHomeTeamName());
            betSelection.setAwayTeamName(gameEntity.getFullAwayTeamName());
            betSelection.setOdd(outcome.getOdd());
            betSelection.setTournamentId(gameEntity.getTournamentId());
            betSelection.setTournamentName(gameEntity.getTournamentName());
            betSelection.setTimestampMsStart(gameEntity.getTimestampStart());
            //betSelection.setShortGameId(Helper.getShortId(betSelection.getGameId()) + "");
            betSelection.setMarketDescription(gameFacade.getMarketDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers()));
            betSelection.setOutcomeDescription(gameFacade.getOutcomeDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId()));
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);


            monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("multipleBet: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();
        request.setUid(uid);

        request.setBetType(BaseBetRequest.BET_TYPE_MULTIPLE);

        try {
            SportBetResponse resp = ticketFacade.onIncomingMultipleBet(TicketManageUnit.PRE_MATCH_BET, request, response);
            setBetResponse(response, resp);
        } catch (Exception e) {
            logger.error("Exception: {}", JSON.toJSONString(e));
            monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：" + e.getMessage() + "。详情：用户令牌=" + request.getAuthToken());
            CodeMessage cm = CodeMessage.parse(e.getMessage());
            response.setCode(cm.getCode());
            response.setMsg(cm.getMsg());
            if (response.getData() != null) {
                response.getData().setMaybeWin(0);
            }
        }

        return response;
    }

    /**
     * Multiple Season下注
     *
     * @param request
     * @return
     */
    public MultipleBetResponse multipleBetSeason(MultipleBetRequest request) {
        MultipleBetResponse response = new MultipleBetResponse();

        if (!Constants.isAppEnabled()) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_SERVICE_IS_NOT_AVAILABLE);
            event.setMessage(BettingFailedEvent.MESSAGE_SERVICE_IS_NOT_AVAILABLE);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);


            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_SERVICE_IS_NOT_AVAILABLE);
            return response;
        }

        if (request == null || request.getSelections() == null || request.getSelections().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (request.getAmount() < commandLineArgs.getMultipleBetLeastAmount()) {
            logger.error("multipleBetSeason failed: bad amount {} not reached least requirement {}", request.getAmount(), commandLineArgs.getSingleBetLeastAmount());
            response.setCode(Constants.CODE_AMOUNT_LIMITED);
            response.setMsg("Minimum total stake should be " + commandLineArgs.getMultipleBetLeastAmount() + "ksh");
            return response;
        }

        for (BetSelection betSelection : request.getSelections()) {
            if (betSelection == null) {
                logger.error("multipleBetSeason failed: at least one selection is null");
                monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：至少一个selection为空");
                response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
                response.setMsg(Constants.ERROR_BAD_REQUEST);
                return response;
            }
        }

        PushMessageBodySport message = checkBetRequestSeason(PushMessageBodySport.MESSAGE_TYPE_MARKET_SEASON, request);
        if (message != null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_MATCH_OR_MARKET_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_MATCH_OR_MARKET_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            logger.error("multipleBetSeason failed: at least on game or market not found");
            monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：至少一个比赛或者Market找不到");
            response.setCode(Constants.CODE_BET_MESSAGE);
            BetResponseData data = new BetResponseData();
            data.setMessage(message);
            response.setData(data);
            return response;
        }

        if (request.getSelections().size() < 2) {
            monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：下注数目少于2。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        String sportId = null;
        String gameId = null;

        for (BetSelection betSelection : request.getSelections()) {
            if (sportId == null) {
                sportId = betSelection.getSportId();
            } else {
                if (!sportId.equals(betSelection.getSportId())) {
                    monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：同一个下注请求中包含了不同的运动类型。详情：用户令牌=" + request.getAuthToken());
                    response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                    response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
                    return response;
                }
            }

            if (gameId == null) {
                gameId = betSelection.getGameId();
            } else {
                if (gameId.equals(betSelection.getGameId())) {
                    monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：同一个下注请求中包含了俩个或者以上相同的比赛。详情：用户令牌=" + request.getAuthToken());
                    response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                    response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
                    return response;
                }
            }

            GameEntity gameEntity = seasonFacade.getSeason(betSelection.getGameId());
            if (gameEntity == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：找不到比赛。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_SEASON, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            GameMarketOutcome outcome = gameEntity.getOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId());
            if (outcome == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：Market未激活或者找不到下注选项。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = checkBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_SEASON, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            betSelection.setHomeTeamName(gameEntity.getFullHomeTeamName());
            betSelection.setAwayTeamName(gameEntity.getFullAwayTeamName());
            betSelection.setOdd(outcome.getOdd());
            betSelection.setTournamentId(gameEntity.getTournamentId());
            betSelection.setTournamentName(gameEntity.getTournamentName());
            betSelection.setTimestampMsStart(gameEntity.getTimestampStart());
            //betSelection.setShortGameId(Helper.getShortId(betSelection.getGameId()) + "");
            betSelection.setMarketDescription(seasonFacade.getMarketDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers()));
            betSelection.setOutcomeDescription(seasonFacade.getOutcomeDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId()));
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("multipleBetSeason: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();
        request.setUid(uid);

        request.setBetType(BaseBetRequest.BET_TYPE_MULTIPLE);

        try {
            SportBetResponse resp = ticketFacade.onIncomingMultipleBet(TicketManageUnit.SEASON_BET, request, response);
            //gameFacade.onIncomingMultipleBet(request);
            setBetResponse(response, resp);
        } catch (Exception e) {
            logger.error("multipleBetSeason Exception: {}", JSON.toJSONString(e));
            monitorFacade.onAlert("【请求分发模块】multipleBetSeason下注请求失败，原因：" + e.getMessage() + "。详情：用户令牌=" + request.getAuthToken());
            CodeMessage cm = CodeMessage.parse(e.getMessage());
            response.setCode(cm.getCode());
            response.setMsg(cm.getMsg());
            if (response.getData() != null) {
                response.getData().setMaybeWin(0);
            }
        }

        return response;
    }

    /**
     * LIVE Multiple下注
     *
     * @param request
     * @return
     */
    public MultipleBetResponse liveMultipleBet(MultipleBetRequest request) {
        MultipleBetResponse response = new MultipleBetResponse();

        if (!Constants.isAppEnabled()) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_SERVICE_IS_NOT_AVAILABLE);
            event.setMessage(BettingFailedEvent.MESSAGE_SERVICE_IS_NOT_AVAILABLE);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_SERVICE_IS_NOT_AVAILABLE);
            return response;
        }

        if (!commandLineArgs.isEnabledLive()) {
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg(Constants.ERROR_NOT_SUPPORT_LIVE_BETTING);
            return response;
        }

        if (request == null || request.getSelections() == null || request.getSelections().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (request.getAmount() < commandLineArgs.getMultipleBetLeastAmount()) {
            logger.error("LIVE multipleBet failed: bad amount {} not reached least requirement {}", request.getAmount(), commandLineArgs.getSingleBetLeastAmount());
            response.setCode(Constants.CODE_AMOUNT_LIMITED);
            response.setMsg("LIVE Minimum total stake should be " + commandLineArgs.getMultipleBetLeastAmount() + "ksh");
            return response;
        }

        for (BetSelection betSelection : request.getSelections()) {
            if (betSelection == null) {
                logger.error("LIVE multipleBet failed: at least one selection is null");
                monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：至少一个selection为空");
                response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
                response.setMsg(Constants.ERROR_BAD_REQUEST);
                return response;
            }
        }

        PushMessageBodySport message = liveCheckBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_LIVE, request);
        if (message != null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_MATCH_OR_MARKET_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_MATCH_OR_MARKET_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            logger.error("LIVE multipleBet failed: at least on game or market not found");
            monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：至少一个比赛或者Market找不到");
            response.setCode(Constants.CODE_BET_MESSAGE);
            BetResponseData data = new BetResponseData();
            data.setMessage(message);
            response.setData(data);
            return response;
        }

        if (request.getSelections().size() < 2) {
            monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：下注数目少于2。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }


        if (containsSameMatches(request.getSelections())){
            monitorFacade.onAlert("【请求分发模块】Multiple下注请求失败，原因：同一个下注请求中包含了俩个或者以上相同的比赛。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
            return response;
        }

        String sportId = null;

        for (BetSelection betSelection : request.getSelections()) {
            if (sportId == null) {
                sportId = betSelection.getSportId();
            } else {
                if (!sportId.equals(betSelection.getSportId())) {
                    monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：同一个下注请求中包含了不同的运动类型。详情：用户令牌=" + request.getAuthToken());
                    response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                    response.setMsg(Constants.ERROR_NOT_ACCEPT_BET);
                    return response;
                }
            }

            GameEntity gameEntity = liveGameFacade.getGame(betSelection.getGameId());
            if (gameEntity == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MATCH_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MATCH_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：找不到比赛。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = liveCheckBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_LIVE, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            GameMarketOutcome outcome = gameEntity.getOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId());
            if (outcome == null) {

                BettingFailedEvent event = new BettingFailedEvent();
                event.setType(BettingFailedEvent.TYPE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setMessage(BettingFailedEvent.MESSAGE_MARKET_OR_OUTCOME_NOT_FOUND);
                event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
                event.setTimestamp(System.currentTimeMillis());
                event.setToken(request.getAuthToken());
                ticketFacade.bettingFailed(event);

                monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：Market未激活或者找不到下注选项。详情：用户令牌=" + request.getAuthToken() + " 比赛ID=" + betSelection.getGameId() + " 下注方式ID=" + betSelection.getMarketId() + " 下注选项ID=" + betSelection.getOutcomeId());
                message = liveCheckBetRequest(PushMessageBodySport.MESSAGE_TYPE_MARKET_LIVE, request);
                response.setCode(Constants.CODE_BET_MESSAGE);
                BetResponseData data = new BetResponseData();
                data.setMessage(message);
                response.setData(data);
                return response;
            }

            betSelection.setHomeTeamName(gameEntity.getFullHomeTeamName());
            betSelection.setAwayTeamName(gameEntity.getFullAwayTeamName());
            betSelection.setOdd(outcome.getOdd());
            betSelection.setTournamentId(gameEntity.getTournamentId());
            betSelection.setTournamentName(gameEntity.getTournamentName());
            betSelection.setTimestampMsStart(gameEntity.getTimestampStart());
            //betSelection.setShortGameId(Helper.getShortId(betSelection.getGameId()) + "");
            betSelection.setMarketDescription(liveGameFacade.getMarketDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers()));
            betSelection.setOutcomeDescription(liveGameFacade.getOutcomeDescription(betSelection.getGameId(), betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId()));
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {

            BettingFailedEvent event = new BettingFailedEvent();
            event.setType(BettingFailedEvent.TYPE_USER_TOKEN_NOT_FOUND);
            event.setMessage(BettingFailedEvent.MESSAGE_USER_TOKEN_NOT_FOUND);
            event.setStatus(BettingFailedEvent.STATUS_NOT_PROCESSED);
            event.setTimestamp(System.currentTimeMillis());
            event.setToken(request.getAuthToken());
            ticketFacade.bettingFailed(event);

            monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("LIVE multipleBet: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();
        request.setUid(uid);

        request.setBetType(BaseBetRequest.BET_TYPE_MULTIPLE);

        try {
            liveGameFacade.onLiveBetting(request);
            SportBetResponse resp = ticketFacade.onIncomingMultipleBet(TicketManageUnit.LIVE_MATCH_BET, request, response);
            setBetResponse(response, resp);
        } catch (Exception e) {
            logger.error("Exception: {}", JSON.toJSONString(e));
            monitorFacade.onAlert("【请求分发模块】LIVE Multiple下注请求失败，原因：" + e.getMessage() + "。详情：用户令牌=" + request.getAuthToken());
            CodeMessage cm = CodeMessage.parse(e.getMessage());
            response.setCode(cm.getCode());
            response.setMsg(cm.getMsg());
            if (response.getData() != null) {
                response.getData().setMaybeWin(0);
            }
        }

        return response;
    }


    private PushMessageBodySport checkBetRequest(int messageType, BaseBetRequest request) {
        List<BetSelection> noGames = null;
        List<BetSelection> noMarkets = null;
        List<BetSelection> noOutcomes = null;
        for (BetSelection betSelection : request.getSelections()) {
            GameEntity gameEntity = gameFacade.getGame(betSelection.getGameId());
            if (gameEntity == null) {
                if (noGames == null) {
                    noGames = new ArrayList<>();
                }
                if (!noGames.contains(betSelection.getGameId())) {
                    noGames.add(betSelection);
                }
            } else {
                if (!gameEntity.containsMarket(betSelection.getMarketId(), betSelection.getSpecifiers())) {
                    if (noMarkets == null) {
                        noMarkets = new ArrayList<>();
                    }
                    if (!noMarkets.contains(betSelection)) {
                        noMarkets.add(betSelection);
                    }
                }
                if (!gameEntity.containsOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId())) {
                    if (noOutcomes == null) {
                        noOutcomes = new ArrayList<>();
                    }

                    if (!noOutcomes.contains(betSelection)) {
                        noOutcomes.add(betSelection);
                    }
                }
            }
        }

        if (noGames != null || noMarkets != null) {
            PushMessageBodySport message = new PushMessageBodySport();
            message.setType(messageType);
            List<MarketMessage> allMessages = new ArrayList<>();

            if (noGames != null) {
                for (BetSelection betSelection : noGames) {
                    MarketMessage msg = new MarketMessage();
                    msg.setMessageType(MarketMessage.MESSAGE_TYPE_GAME_REMOVED);
                    msg.setSportId(betSelection.getSportId());
                    msg.setGameId(betSelection.getGameId());
                    allMessages.add(msg);
                }
            }
            if (noMarkets != null) {
                for (BetSelection betSelection : noMarkets) {
                    MarketMessage msg = new MarketMessage();
                    msg.setMessageType(MarketMessage.MESSAGE_TYPE_MARKET_REMOVED);
                    msg.setSportId(betSelection.getSportId());
                    msg.setGameId(betSelection.getGameId());
                    msg.setMarketId(betSelection.getMarketId());
                    msg.setMarketSpecifiers(betSelection.getSpecifiers());
                    allMessages.add(msg);
                }
            }
            if (noOutcomes != null) {
                for (BetSelection betSelection : noOutcomes) {
                    MarketMessage marketMessage = new MarketMessage();
                    marketMessage.setMessageType(MarketMessage.MESSAGE_TYPE_OUTCOME_REMOVED);
                    marketMessage.setSportId(betSelection.getSportId());
                    marketMessage.setGameId(betSelection.getGameId());
                    marketMessage.setMarketId(betSelection.getMarketId());
                    marketMessage.setMarketSpecifiers(betSelection.getSpecifiers());

                    List<OutcomeMessage> outcomeMessages = new ArrayList<>();
                    marketMessage.setOutcomeMessages(outcomeMessages);

                    OutcomeMessage outcomeMessage = new OutcomeMessage();
                    outcomeMessage.setId(betSelection.getOutcomeId());
                    outcomeMessages.add(outcomeMessage);

                    allMessages.add(marketMessage);
                }
            }
            message.setData(allMessages);
            return message;
        }

        return null;
    }

    private PushMessageBodySport checkBetRequestSeason(int messageType, BaseBetRequest request) {
        List<BetSelection> noGames = null;
        List<BetSelection> noMarkets = null;
        List<BetSelection> noOutcomes = null;
        for (BetSelection betSelection : request.getSelections()) {
            GameEntity gameEntity = seasonFacade.getSeason(betSelection.getGameId());
            if (gameEntity == null) {
                if (noGames == null) {
                    noGames = new ArrayList<>();
                }
                if (!noGames.contains(betSelection.getGameId())) {
                    noGames.add(betSelection);
                }
            } else {
                if (!gameEntity.containsMarket(betSelection.getMarketId(), betSelection.getSpecifiers())) {
                    if (noMarkets == null) {
                        noMarkets = new ArrayList<>();
                    }
                    if (!noMarkets.contains(betSelection)) {
                        noMarkets.add(betSelection);
                    }
                }
                if (!gameEntity.containsOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId())) {
                    if (noOutcomes == null) {
                        noOutcomes = new ArrayList<>();
                    }

                    if (!noOutcomes.contains(betSelection)) {
                        noOutcomes.add(betSelection);
                    }
                }
            }
        }

        if (noGames != null || noMarkets != null) {
            PushMessageBodySport message = new PushMessageBodySport();
            message.setType(messageType);
            List<MarketMessage> allMessages = new ArrayList<>();

            if (noGames != null) {
                for (BetSelection betSelection : noGames) {
                    MarketMessage msg = new MarketMessage();
                    msg.setMessageType(MarketMessage.MESSAGE_TYPE_GAME_REMOVED);
                    msg.setSportId(betSelection.getSportId());
                    msg.setGameId(betSelection.getGameId());
                    allMessages.add(msg);
                }
            }
            if (noMarkets != null) {
                for (BetSelection betSelection : noMarkets) {
                    MarketMessage msg = new MarketMessage();
                    msg.setMessageType(MarketMessage.MESSAGE_TYPE_MARKET_REMOVED);
                    msg.setSportId(betSelection.getSportId());
                    msg.setGameId(betSelection.getGameId());
                    msg.setMarketId(betSelection.getMarketId());
                    msg.setMarketSpecifiers(betSelection.getSpecifiers());
                    allMessages.add(msg);
                }
            }
            if (noOutcomes != null) {
                for (BetSelection betSelection : noOutcomes) {
                    MarketMessage marketMessage = new MarketMessage();
                    marketMessage.setMessageType(MarketMessage.MESSAGE_TYPE_OUTCOME_REMOVED);
                    marketMessage.setSportId(betSelection.getSportId());
                    marketMessage.setGameId(betSelection.getGameId());
                    marketMessage.setMarketId(betSelection.getMarketId());
                    marketMessage.setMarketSpecifiers(betSelection.getSpecifiers());

                    List<OutcomeMessage> outcomeMessages = new ArrayList<>();
                    marketMessage.setOutcomeMessages(outcomeMessages);

                    OutcomeMessage outcomeMessage = new OutcomeMessage();
                    outcomeMessage.setId(betSelection.getOutcomeId());
                    outcomeMessages.add(outcomeMessage);

                    allMessages.add(marketMessage);
                }
            }
            message.setData(allMessages);
            return message;
        }

        return null;
    }

    private PushMessageBodySport liveCheckBetRequest(int messageType, BaseBetRequest request) {
        List<BetSelection> noGames = null;
        List<BetSelection> noMarkets = null;
        List<BetSelection> noOutcomes = null;

        for (BetSelection betSelection : request.getSelections()) {
            GameEntity gameEntity = liveGameFacade.getGame(betSelection.getGameId());
            if (gameEntity == null) {
                if (noGames == null) {
                    noGames = new ArrayList<>();
                }
                if (!noGames.contains(betSelection.getGameId())) {
                    noGames.add(betSelection);
                }
            } else {
                if (!gameEntity.containsMarket(betSelection.getMarketId(), betSelection.getSpecifiers())) {
                    if (noMarkets == null) {
                        noMarkets = new ArrayList<>();
                    }
                    if (!noMarkets.contains(betSelection)) {
                        noMarkets.add(betSelection);
                    }
                }
                if (!gameEntity.containsOutcome(betSelection.getMarketId(), betSelection.getSpecifiers(), betSelection.getOutcomeId())) {
                    if (noOutcomes == null) {
                        noOutcomes = new ArrayList<>();
                    }

                    if (!noOutcomes.contains(betSelection)) {
                        noOutcomes.add(betSelection);
                    }
                }
            }
        }

        if (noGames != null || noMarkets != null) {
            PushMessageBodySport message = new PushMessageBodySport();
            message.setType(messageType);
            List<MarketMessage> allMessages = new ArrayList<>();

            if (noGames != null) {
                for (BetSelection betSelection : noGames) {
                    MarketMessage msg = new MarketMessage();
                    msg.setMessageType(MarketMessage.MESSAGE_TYPE_GAME_REMOVED);
                    msg.setSportId(betSelection.getSportId());
                    msg.setGameId(betSelection.getGameId());
                    allMessages.add(msg);
                }
            }
            if (noMarkets != null) {
                for (BetSelection betSelection : noMarkets) {
                    MarketMessage msg = new MarketMessage();
                    msg.setMessageType(MarketMessage.MESSAGE_TYPE_MARKET_REMOVED);
                    msg.setSportId(betSelection.getSportId());
                    msg.setGameId(betSelection.getGameId());
                    msg.setMarketId(betSelection.getMarketId());
                    msg.setMarketSpecifiers(betSelection.getSpecifiers());
                    allMessages.add(msg);
                }
            }

            if (noOutcomes != null) {
                for (BetSelection betSelection : noOutcomes) {
                    MarketMessage marketMessage = new MarketMessage();
                    marketMessage.setMessageType(MarketMessage.MESSAGE_TYPE_OUTCOME_REMOVED);
                    marketMessage.setSportId(betSelection.getSportId());
                    marketMessage.setGameId(betSelection.getGameId());
                    marketMessage.setMarketId(betSelection.getMarketId());
                    marketMessage.setMarketSpecifiers(betSelection.getSpecifiers());

                    List<OutcomeMessage> outcomeMessages = new ArrayList<>();
                    marketMessage.setOutcomeMessages(outcomeMessages);

                    OutcomeMessage outcomeMessage = new OutcomeMessage();
                    outcomeMessage.setId(betSelection.getOutcomeId());
                    outcomeMessages.add(outcomeMessage);

                    allMessages.add(marketMessage);
                }
            }

            message.setData(allMessages);
            return message;
        }

        return null;
    }

    private void setBetResponse(BaseResponse response1, SportBetResponse response2) {
        if (response2 != null) {
            if (response2.getCode() == 0) {
                response1.setCode(Constants.CODE_SUCCESSFUL);
            } else {
                response1.setCode(response2.getCode());
                response1.setMsg(response2.getMsg());
            }
        } else {
            response1.setCode(Constants.CODE_APP_DISABLED);
            response1.setMsg(Constants.ERROR_SERVICE_IS_NOT_AVAILABLE);
        }
    }

    /**
     * Double
     *
     * @param request
     * @return
     */
    public DoubleBetResponse doubleBet(DoubleBetRequest request) {
        DoubleBetResponse response = new DoubleBetResponse();
        return response;
    }

    private List<String> getEndedGames(List<BetSelection> selections) {
        List<String> endedGames = new ArrayList<>();
        long nowMs = System.currentTimeMillis();
        for (BetSelection betSelection : selections) {
            GameEntity gameEntity = gameFacade.getGame(betSelection.getGameId());
            // FIXME: 在实时投注中，比赛开始后，是不要删除的
            if (gameEntity == null || gameEntity.getGameStatus() >= EventStatus.Ended.ordinal() || nowMs - gameEntity.getTimestampStart() > 0) {
                endedGames.add(betSelection.getGameId());
            }
        }
        return endedGames;
    }

    private List<String> getLiveEndedGames(List<BetSelection> selections) {
        List<String> endedGames = new ArrayList<>();
        long nowMs = System.currentTimeMillis();
        for (BetSelection betSelection : selections) {
            GameEntity gameEntity = liveGameFacade.getGame(betSelection.getGameId());
            if (gameEntity == null || gameEntity.getGameStatus() >= EventStatus.Ended.ordinal()) {
                endedGames.add(betSelection.getGameId());
            }
        }
        return endedGames;
    }

    private String getEndedGamesString(List<String> endedGames) {
        String games = "";
        if (endedGames != null) {
            for (String id : endedGames) {
                games += id + ",";
            }
        }
        return games;
    }

    /**
     * 获取历史Ticket
     *
     * @param request
     * @return
     */
    public HistoryTicketsResponse historyTickets(HistoryTicketsRequest request) {
        HistoryTicketsResponse response = new HistoryTicketsResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("historyTickets: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();

        response.setCode(Constants.CODE_SUCCESSFUL);
        List<TicketManageUnit> ticketManageUnits = ticketFacade.historyTickets(uid, request.getTimeBegin(), request.getTimeEnd());

        // 只返回少量数据，减少网络传输量
        if (ticketManageUnits != null && ticketManageUnits.size() > 0) {
            for (TicketManageUnit ticketManageUnit : ticketManageUnits) {
                if (ticketManageUnit.hasAllRemoved()){
                    ticketManageUnit.setStatus(TicketManageUnit.STATUS_ALL_MATCHES_CLEARED);
                }
                ticketManageUnit.setMaybeWin(ticketManageUnit.calcuMaybeWin());
                if (ticketManageUnit.getRequest().getBetType() != BaseBetRequest.BET_TYPE_SINGLE) {
                    if (!request.isComplete()){
                        ticketManageUnit.getRequest().setSelections(null);
                    }
                }else {
                    for (BetSelection selection : ticketManageUnit.getRequest().getSelections()){
                        if (selection.getTournamentId() == null || selection.getTournamentId().isEmpty()){
                            MySimpleTournament tournament = mongoTemplate.findOne(new Query(Criteria.where("game_id").is(selection.getGameId())), MySimpleTournament.class, GameEntity.MONGO_COLLECTION_NAME);
                            if (tournament != null){
                                selection.setTournamentId(tournament.getId());
                                selection.setTournamentName(tournament.getName());
                            }
                        }
                    }
                }
            }
        }

        response.setData(ticketManageUnits);

        return response;
    }

    private void fillScore(List<TicketManageUnit> ticketManageUnits) {
        if (ticketManageUnits == null) {
            return;
        }

        for (TicketManageUnit ticketManageUnit : ticketManageUnits) {
            for (BetSelection betSelection : ticketManageUnit.getRequest().getSelections()) {
                if (betSelection.getStatus() == BetSelection.STATUS_GAP_SETTLEMENT) {
                    betSelection.setGameResult(gameFacade.getGameResult(betSelection.getGameId()));
                }
            }
        }
    }

    /**
     * 管理员获取历史Ticket
     *
     * @param request
     * @return
     */
    public HistoryTicketsResponse historyTicketsAdmin(HistoryTicketsRequest request) {
        HistoryTicketsResponse response = new HistoryTicketsResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        response.setCode(Constants.CODE_SUCCESSFUL);
        List<TicketManageUnit> ticketManageUnits = ticketFacade.historyTicketsAdmin(request.getTimeBegin(), request.getTimeEnd());
        fillScore(ticketManageUnits);
        response.setData(ticketManageUnits);

        return response;
    }

    /**
     * 获取比赛
     *
     * @param request
     * @return
     */
    public GamesResponse games(GamesRequest request) {
        GamesResponse response = new GamesResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        response.setCode(Constants.CODE_SUCCESSFUL);
        response.setData(gameFacade.games(request));

        return response;
    }

    /**
     * 更新比赛信息
     *
     * @param request
     * @return
     */
    public UpdateGameResponse updateGame(UpdateGameRequest request) {
        UpdateGameResponse response = new UpdateGameResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        gameFacade.updateGame(request.getData());

        response.setCode(Constants.CODE_SUCCESSFUL);

        return response;
    }

    /**
     * 查询Ticket事件状态
     *
     * @param request
     * @return
     */
    public TicketEventResponse ticketEvent(TicketEventRequest request) {
        TicketEventResponse response = new TicketEventResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        response = ticketFacade.ticketEvent(request);
        response.setCode(Constants.CODE_SUCCESSFUL);

        return response;
    }

    /**
     * 恢复事件赔率
     *
     * @param request
     * @return
     */
    public RecoveryOddsResponse recoveryOdds(RecoveryOddsRequest request) {
        RecoveryOddsResponse response = new RecoveryOddsResponse();
        if (request == null || request.getEventId() == null || request.getEventId().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        oddsFeedFacade.recoveryOdds(request);

        response.setCode(Constants.CODE_SUCCESSFUL);

        return response;
    }

    /**
     * 恢复事件状态
     *
     * @param request
     * @return
     */
    public RecoveryStatefulResponse recoveryStateful(RecoveryStatefulRequest request) {
        RecoveryStatefulResponse response = new RecoveryStatefulResponse();
        if (request == null || request.getEventId() == null || request.getEventId().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        oddsFeedFacade.recoveryStateful(request);

        response.setCode(Constants.CODE_SUCCESSFUL);

        return response;
    }

    /**
     * 查询比赛状态
     *
     * @param request
     * @return
     */
    public MatchStatusResponse matchStatus(MatchStatusRequest request) {
        MatchStatusResponse response = new MatchStatusResponse();
        if (request == null || request.getId() == null || request.getId().isEmpty()) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        response = gameFacade.matchStatus(request.getId());
        response.setCode(Constants.CODE_SUCCESSFUL);

        return response;
    }

    /**
     * 查询比赛状态
     *
     * @param request
     * @return
     */
    public TestPushMessageResponse testPushMessage(TestPushMessageRequest request) {
        TestPushMessageResponse response = new TestPushMessageResponse();
        response.setCode(Constants.CODE_SUCCESSFUL);

        return response;
    }

    public SendSMSResponse sendSMS(SendSMSRequest request) {
        return accountFacade.sendSMS(request);
    }

    /**
     * 获取Ticket详情
     *
     * @param request
     * @return
     */
    public TicketDetailResponse ticketDetail(TicketDetailRequest request) {
        TicketDetailResponse response = new TicketDetailResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("ticketDetail: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();

        response.setCode(Constants.CODE_SUCCESSFUL);
        TicketManageUnit ticket = ticketFacade.ticketDetail(uid, request.getTicketId());
        List<TicketManageUnit> ticketManageUnits = new ArrayList<>();
        ticketManageUnits.add(ticket);
        fillScore(ticketManageUnits);
        for (BetSelection selection : ticket.getRequest().getSelections()){
            if (selection.getTournamentId() == null || selection.getTournamentId().isEmpty()){
                MySimpleTournament tournament = mongoTemplate.findOne(new Query(Criteria.where("game_id").is(selection.getGameId())), MySimpleTournament.class, GameEntity.MONGO_COLLECTION_NAME);
                if (tournament != null){
                    selection.setTournamentId(tournament.getId());
                    selection.setTournamentName(tournament.getName());
                }
            }
        }
        response.setData(ticketManageUnits);

        return response;
    }

    public GameDelayReportResponse gameDelayReport(GameDelayReportRequest request) {
        logger.info("gameDelayReport: {}", JSON.toJSONString(request));
        GameDelayReportResponse response = new GameDelayReportResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (!request.getAccessToken().equals(commandLineArgs.getAccessTokenInternal())) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        }

        ticketFacade.gameDelayReport(request);

        return response;
    }

    public ManualMultipleBetSettlementResponse manualMultipleBetSettlement(ManualMultipleBetSettlementRequest request) {
        ManualMultipleBetSettlementResponse response = new ManualMultipleBetSettlementResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (!request.getAccessToken().equals(commandLineArgs.getAccessTokenInternal())) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        }

        ticketFacade.manualMultipleBetSettlement(request);

        return response;
    }

    public ManualSingleBetSettlementResponse manualSingleBetSettlement(ManualSingleBetSettlementRequest request) {
        ManualSingleBetSettlementResponse response = new ManualSingleBetSettlementResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (!request.getAccessToken().equals(commandLineArgs.getAccessTokenInternal())) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        }

        ticketFacade.manualSingleBetSettlement(request);

        return response;
    }

    public SubscribeGameResponse subscribeGame(SubscribeGameRequest request) {
        SubscribeGameResponse response = new SubscribeGameResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {
            monitorFacade.onAlert("【请求分发模块】订阅LIVE比赛推送请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {
            monitorFacade.onAlert("【请求分发模块】订阅LIVE比赛推送请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("subscribeGame: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();

        pushFacade.subscribeGame(request.getAuthToken(), uid, request.getGameId());

        return response;
    }

    public UnsubscribeGameResponse unsubscribeGame(UnsubscribeGameRequest request) {
        UnsubscribeGameResponse response = new UnsubscribeGameResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {
            monitorFacade.onAlert("【请求分发模块】取消订阅LIVE比赛推送请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {
            monitorFacade.onAlert("【请求分发模块】取消订阅LIVE比赛推送请求失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        } else {
            logger.info("unsubscribeGame: accountFacade getUserInfo succeed, request: {}, response: {}", JSON.toJSONString(userInfoRequest), JSON.toJSONString(userInfoResponse));
        }

        String uid = userInfoResponse.getData().getUid();

        pushFacade.unsubscribeGame(request.getAuthToken(), uid, request.getGameId());

        return response;
    }

    public SportWinResponse sportWin(SportWinRequest request) {
        SportWinResponse response = new SportWinResponse();
        if (accountFacade.sportWin(request.isGrantGold(), request.getTicketId(), request.getUid(), request.getAmount(), request.getBetAmount())) {
            logger.info("test sportWin succeed");
        } else {
            logger.info("test sportWin failed");
            response.setCode(-1);
        }
        return response;
    }

    public SportRefundResponse sportRefund(SportRefundRequest request) {
        SportRefundResponse response = new SportRefundResponse();
        if (accountFacade.sportRefund(request.isGrantGold(), request.getTicketId(), request.getUid(), request.getAmount(), 0, Constants.SMS_REFUND_NORMAL, 0)) {
            logger.info("test sportRefund succeed");
        } else {
            logger.info("test sportRefund failed");
            response.setCode(-1);
        }
        return response;
    }

    public FutureLiveCompetitionsResponse futureLiveCompetitions(FutureLiveCompetitionsRequest request) {
        FutureLiveCompetitionsResponse response = new FutureLiveCompetitionsResponse();
        List<SimpleCompetition> competitions = liveGameFacade.getFutureLiveCompetitions(request);
        response.setData(competitions);
        response.setCode(Constants.CODE_SUCCESSFUL);
        return response;
    }

    public CompetitionsWithResultsResponse competitionsWithResults(CompetitionsWithResultsRequest request) {
        CompetitionsWithResultsResponse response = new CompetitionsWithResultsResponse();
        List<CompetitionWithResults> competitions = resultsManager.getCompetitions(request);
        response.setData(competitions);
        return response;
    }

    public RecommandCompetitionsResponse recommandCompetitions(RecommandCompetitionsRequest request) {
        RecommandCompetitionsResponse response = new RecommandCompetitionsResponse();
        if (request == null) {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            response.setMsg(Constants.ERROR_BAD_REQUEST);
            return response;
        }

        if (!request.getAccessToken().equals(commandLineArgs.getAccessTokenInternal())) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        }

        if (request.isLive()) {
            if (request.getCompetitions() != null) {
                List<GameEntity> games = new ArrayList<>();
                for (String gameId : request.getCompetitions()) {
                    GameEntity game = gameFacade.getGame(gameId);
                    if (game == null) {
                        logger.error("Not found competition {}", gameId);
                        continue;
                    }
                    if (!games.contains(game)) {
                        games.add(game);
                    }
                }
                liveGameFacade.setHighlightsGames(games);
            }
        } else {
            gameFacade.recommandCompetitions(request.getCompetitions());
        }

        return response;
    }

    public NotSettlementResponse<NotSettlement> notSettlement(NotSettlementRequest request) {
        NotSettlementResponse<NotSettlement> response = new NotSettlementResponse<>();

        if (request.getAuthToken() == null ||
                request.getAuthToken().length() == 0) {
            response.setData(null);
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            return response;
        }

        if (!request.getAuthToken().equals
                (commandLineArgs.getSecretKey())) {
            response.setData(null);
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            return response;
        }

        if (request.getPage() < 1) {
            response.setData(null);
            response.setCode(Constants.PAGE_ILLEGAL);
            return response;
        }

        if (request.getPageSize() <= 0) {
            request.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }

        NotSettlementDTO notSettlements =
                gameFacade.notSettlement(request.getPage(), request.getPageSize());
        response.setCode(0);
        response.setData(notSettlements.getData());
        response.setPageCount(notSettlements.getPageCount());

        logger.info("notSettlement response: {}", JSON.toJSONString(response));
        return response;
    }

    public BaseResponse betikaSettlement(BetikaSettlementRequest request) {
        BaseResponse response = new BaseResponse();

        logger.info("request of betikaSettlement: {}", JSON.toJSONString(request));

        if (request.getAuthToken() == null ||
                request.getAuthToken().length() == 0) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            return response;
        }

        if (!request.getAuthToken().equals
                (commandLineArgs.getSecretKey())) {
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            return response;
        }

        if (Strings.isNullOrEmpty(request.getGameId())) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            return response;
        }

        if (Strings.isNullOrEmpty(request.getProduct())) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            return response;
        }

        if (Strings.isNullOrEmpty(request.getGameResult())) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            return response;
        }

        if (request.getItems() == null || request.getItems().size() <= 0) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            return response;
        }

        if (request.getGameStatus() <= 0) {
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            return response;
        }

        // set by gamemania
        request.setStatus(BetSettlementRequest.STATUS_PROCESSED);
        request.setTimestampMs(System.currentTimeMillis());
        request.setTimestampMsStatus(System.currentTimeMillis());

        request.setWarpRequest(request);

        ticketFacade.onIncomingBetSettlementRequest
                (request.getWarpRequest());

        response.setCode(0);
        response.setMsg("ok");

        return response;
    }

    public TicketResultResponse ticketResultCallback(TicketResultRequest request){
        TicketResultResponse response = new TicketResultResponse();
        if (!commandLineArgs.getAccessTokenTicketResultCallback().equals(request.getAccessToken())){
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg("bad access token");
            return response;
        }

        switch (request.getStatus()){
            case TicketResultRequest.STATUS_ACCEPTED:
                ticketFacade.onTicketSendResponseIsAccepted(request.getTicketId());
                break;
            case TicketResultRequest.STATUS_REJECTED:
                ticketFacade.onTicketSendResponseIsRejected(request.getTicketId(), request.getMessage());
                break;
            case TicketResultRequest.STATUS_CANCELLED:
                ticketFacade.onTicketSendCancelResponseIsCancelled(request.getTicketId());
                break;
            case TicketResultRequest.STATUS_NOT_CANCELLED:
                ticketFacade.onTicketSendCancelResponseIsNotCancelled(request.getTicketId(), request.getMessage());
                break;
            case TicketResultRequest.STATUS_NO_RESPONSE:
                ticketFacade.onTicketSendResponseIsNotReceived(request.getTicketId());
                break;
            default:
                response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
                response.setMsg("bad status");
                break;
        }

        return response;
    }

    public SeasonResponse season(SeasonRequest request){
        SeasonResponse response = new SeasonResponse();
        response.setData(seasonFacade.getSeason(request.getStartIndex(), request.getPageCount()));
        return response;
    }

    public BetCtlResponse betCtl(BetCtlRequest request){
        BetCtlResponse response = new BetCtlResponse();

        try {
            gameFacade.betCtl(request, response);
            liveGameFacade.betCtl(request, response);
        }catch (Exception e){
            response.setCode(Constants.CODE_REQUEST_NOT_ALLOWED);
            response.setMsg(e.getMessage());
        }

        return response;
    }

    public FindOddsResponse findOdds(FindOddsRequest request){
        FindOddsResponse response = new FindOddsResponse();

        if (request.getItems() != null && request.getItems().size() > 0){
            for (OutcomeItem2 outcomeItem : request.getItems()){
                // prematch
                GameEntity gameEntity = gameFacade.getGame(outcomeItem.getGameId());
                if (gameEntity == null){
                    // live
                    gameEntity = liveGameFacade.getGame(outcomeItem.getGameId());
                }
                if (gameEntity == null){
                    // outrights
                    // worldcup 2018
                    gameEntity = seasonFacade.getSeason(outcomeItem.getGameId());
                }
                if (gameEntity != null){
                    if(!gameEntity.updateOdds(outcomeItem)){
                        outcomeItem.setOutcomeStatus(OutcomeItem2.DEACTIVE);
                    }
                }else {
                    outcomeItem.setOutcomeStatus(OutcomeItem2.DEACTIVE);
                }
            }
        }else {
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
        }

        response.setCode(Constants.CODE_SUCCESSFUL);
        response.setData(request.getItems());

        return response;
    }

    public ManualOddsChangeResponse manualOddsChange(ManualOddsChangeRequest request){
        ManualOddsChangeResponse response = new ManualOddsChangeResponse();
        gameFacade.handleNormalOddsChange(request.getMessage());
        response.setCode(Constants.CODE_SUCCESSFUL);
        return response;
    }

    public SearchGamesResponse searchGames(SearchGamesRequest request){
        SearchGamesResponse response = new SearchGamesResponse();
        if (request.getKeyword() == null || request.getKeyword().isEmpty() || request.getSportId() == null || request.getSportId().isEmpty()){
            response.setCode(Constants.CODE_REQUEST_FORMAT_ERROR);
            return response;
        }

        List<GameMarketLine> data = new ArrayList<>();
        data.addAll(gameFacade.searchGames(request));
        data.addAll(liveGameFacade.searchGames(request));
        response.setData(data);
        response.setCode(Constants.CODE_SUCCESSFUL);
        return response;
    }

    public ReloadNotSettledTicketResponse reloadNotSettledTicket(ReloadNotSettledTicketRequest request){
        ReloadNotSettledTicketResponse response = new ReloadNotSettledTicketResponse();

        ticketFacade.reloadNotSettlementTicket();

        return response;
    }

    public DefaultSportResponse defaultSport(DefaultSportRequest request){
        DefaultSportResponse response = new DefaultSportResponse();
        DefaultSportResponseData data = new DefaultSportResponseData();
        response.setData(data);
        data.setSportId(liveGameFacade.getDefaultSport());
        response.setCode(Constants.CODE_SUCCESSFUL);
        return response;
    }

    public CashOutRangeResponse cashOutRange(CashOutRangeRequest request){
        CashOutRangeResponse response = new CashOutRangeResponse();
        response.setData(new CashOutRangeResponseData());

        cashOutFacade.cashOutRange(request, response);

        return response;
    }

    public CashOutResponse cashOut(CashOutRequest request){
        CashOutResponse response = new CashOutResponse();
        response.setData(new CashOutResponseData());

        GetUserInfoRequest userInfoRequest = new GetUserInfoRequest();
        userInfoRequest.setToken(request.getAuthToken());
        GetUserInfoResponse userInfoResponse = accountFacade.getUserInfo(userInfoRequest);
        if (userInfoResponse == null) {
            monitorFacade.onAlert("【请求分发模块】Cash Out 失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(Constants.ERROR_AUTH_FAILED);
            return response;
        } else if (userInfoResponse.getCode() != AccountBaseResponse.StatusOK) {
            monitorFacade.onAlert("【请求分发模块】Cash Out 失败，原因：获取用户信息失败。详情：用户令牌=" + request.getAuthToken());
            response.setCode(Constants.CODE_TOKEN_EXPIRED);
            response.setMsg(userInfoResponse.getMsg());
            return response;
        }

        String uid = userInfoResponse.getData().getUid();
        request.setUid(uid);

        cashOutFacade.cashOut(request, response);

        return response;
    }

    public QueryOddsResponse queryOdds(QueryOddsRequest request){
        if (request.getLive() == TicketManageUnit.PRE_MATCH_BET){
            return gameFacade.queryOdds(request);
        }else {
            return liveGameFacade.queryOdds(request);
        }
    }

    private void initMarketCategory(){

    }

    public SportsNumberResponse sportsNumber(SportsNumberRequest request){
        return liveGameFacade.sportsNumber(request);
    }

    public ClearGamesResponse clearGames(ClearGamesRequest request){
        return ticketFacade.clearGames(request);
    }

    private String getCategory(int marketId){
        String main = "Main";
        String goals = "Goals";
        String half = "Half";
        String booking = "Booking";
        String corners = "Corners";
        String specials = "Specials";
        String combo = "Combo";

        switch (marketId){
            case 184: return combo;
            case 37: return combo;
            case 35: return combo;
            case 36: return combo;
            case 79: return combo;
            case 78: return combo;
            case 546: return combo;
            case 542: return combo;
            case 545: return combo;
            case 540: return combo;
            case 541: return combo;
            case 544: return combo;
            case 543: return combo;
            case 547: return combo;
            case 818: return combo;
            case 819: return combo;

            case 1: return main;
            case 18: return main;
            case 16: return main;
            case 47: return main;
            case 10: return main;
            case 45: return main;

            case 8: return goals;
            case 29: return goals;
            case 21: return goals;
            case 26: return goals;
            case 19: return goals;
            case 20: return goals;
            case 30: return goals;
            case 23: return goals;
            case 24: return goals;
            case 25: return goals;
            case 27: return goals;
            case 28: return goals;
            case 52: return goals;
            case 31: return goals;
            case 32: return goals;
            case 56: return goals;
            case 57: return goals;
            case 58: return goals;
            case 59: return goals;
            case 53: return goals;
            case 54: return goals;

            case 60: return half;
            case 68: return half;
            case 66: return half;
            case 63: return half;
            case 64: return half;
            case 81: return half;
            case 75: return half;
            case 69: return half;
            case 70: return half;
            case 62: return half;
            case 76: return half;
            case 77: return half;
            case 74: return half;
            case 65: return half;
            case 159: return half;
            case 160: return half;
            case 161: return half;
            case 83: return half;
            case 90: return half;
            case 85: return half;
            case 86: return half;
            case 98: return half;
            case 95: return half;
            case 91: return half;
            case 92: return half;
            case 84: return half;
            case 96: return half;
            case 97: return half;
            case 94: return half;
            case 87: return half;

            case 139: return booking;
            case 151: return booking;
            case 152: return booking;
            case 149: return booking;
            case 150: return booking;
            case 143: return booking;
            case 144: return booking;
            case 156: return booking;
            case 157: return booking;
            case 136: return booking;
            case 142: return booking;
            case 155: return booking;
            case 146: return booking;
            case 147: return booking;
            case 148: return booking;

            case 166: return corners;
            case 163: return corners;
            case 177: return corners;
            case 174: return corners;
            case 165: return corners;
            case 176: return corners;
            case 162: return corners;
            case 173: return corners;
            case 172: return corners;
            case 183: return corners;
            case 164: return corners;
            case 170: return corners;
            case 171: return corners;
            case 169: return corners;
            case 182: return corners;



            default: return specials;
        }
    }
}