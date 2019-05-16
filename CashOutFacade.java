package cc.gamemania.modules.cashout;

import cc.gamemania.constants.Constants;
import cc.gamemania.http.api.proto.*;
import cc.gamemania.http.api.proto.entities.BetSelection;
import cc.gamemania.modules.account.ExternalInterface;
import cc.gamemania.modules.game.GameFacade;
import cc.gamemania.modules.game.LiveGameFacade;
import cc.gamemania.modules.game.api.proto.entities.GameMarketOutcome;
import cc.gamemania.modules.ticket.TicketFacade;
import cc.gamemania.modules.ticket.api.proto.entities.TicketManageUnit;
import com.alibaba.fastjson.JSON;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class CashOutFacade implements ApplicationListener<ContextRefreshedEvent> {
    private org.slf4j.Logger logger = LoggerFactory.getLogger("CashOutFacade");

    @Autowired
    private GameFacade gameFacade;

    @Autowired
    private LiveGameFacade liveGameFacade;

    @Autowired
    private TicketFacade ticketFacade;

    @Autowired
    private ExternalInterface accountFacade;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

    }

    public void cashOutRange(CashOutRangeRequest request, CashOutRangeResponse response){

        response.setCode(Constants.CODE_SUCCESSFUL);

        TicketManageUnit ticket = ticketFacade.getTicket(request.getTicketId());
        if (ticket == null){
            logger.error("can not cash out because not found ticket {}", JSON.toJSONString(request));
            response.getData().setMin(0.0);
            response.getData().setMax(0.0);
            return;
        }

        if (ticket.getRequest().getCouponId() > 0){
            logger.error("can not cash out because contains coupon {}", JSON.toJSONString(request));
            response.getData().setMin(0.0);
            response.getData().setMax(0.0);
            return;
        }

        if (ticket.getRequest().isGrantGold()){
            logger.error("can not cash out because is grant gold {}", JSON.toJSONString(request));
            response.getData().setMin(0.0);
            response.getData().setMax(0.0);
            return;
        }

        if (ticket.getRequest().getBetType() == BaseBetRequest.BET_TYPE_SINGLE){
            if (request.getGameId() == null || request.getGameId().isEmpty()){
                logger.error("can not cash out because game id is null {}", JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }

            if (request.getMarketId() == 0){
                logger.error("can not cash out because market id is 0 {}", JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }

            if (request.getOutcomeId() == null || request.getOutcomeId().isEmpty()){
                logger.error("can not cash out because outcome id is null {}", JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }
        }

        double odds = 1.0;

        if (ticket.getIsLive() == TicketManageUnit.LIVE_MATCH_BET){
            // Live

            // Single
            if (ticket.getRequest().getBetType() == BaseBetRequest.BET_TYPE_SINGLE){
                BetSelection selection = liveGameFacade.getSelection(request.getGameId(), request.getMarketId(), request.getSpecifiers(), request.getOutcomeId());
                if (selection == null || selection.getStatus() != GameMarketOutcome.STATUS_ACTIVE){
                    logger.error("LIVE Single can not cash out because market or outcome status is bad, selection={} {}", JSON.toJSONString(selection), JSON.toJSONString(request));
                    response.getData().setMin(0.0);
                    response.getData().setMax(0.0);
                    return;
                }

                odds = selection.getOdd();
            }else {
                // Multiple
                for (BetSelection selection : ticket.getRequest().getSelections()){
                    BetSelection _selection = liveGameFacade.getSelection(selection.getGameId(), selection.getMarketId(), selection.getSpecifiers(), selection.getOutcomeId());
                    if (_selection == null || _selection.getStatus() != GameMarketOutcome.STATUS_ACTIVE){
                        logger.error("LIVE Multiple can not cash out because market or outcome status is bad, selection={} {}", JSON.toJSONString(_selection), JSON.toJSONString(request));
                        response.getData().setMin(0.0);
                        response.getData().setMax(0.0);
                        return;
                    }

                    odds *= _selection.getOdd();
                }

            }

            if (odds == 1.0){
                logger.error("LIVE can not cash out because not found odds {}", JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }
        }else {
            // PreMatch

            // Single
            if (ticket.getRequest().getBetType() == BaseBetRequest.BET_TYPE_SINGLE){
                BetSelection selection = gameFacade.getSelection(request.getGameId(), request.getMarketId(), request.getSpecifiers(), request.getOutcomeId());
                if (selection == null || selection.getStatus() != GameMarketOutcome.STATUS_ACTIVE){
                    logger.error("PreMatch Single can not cash out because market or outcome status is bad, selection={} {}", JSON.toJSONString(selection), JSON.toJSONString(request));
                    response.getData().setMin(0.0);
                    response.getData().setMax(0.0);
                    return;
                }

                odds = selection.getOdd();
            }else {
                // Multiple
                for (BetSelection selection : ticket.getRequest().getSelections()){
                    BetSelection _selection = gameFacade.getSelection(selection.getGameId(), selection.getMarketId(), selection.getSpecifiers(), selection.getOutcomeId());
                    if (_selection == null || _selection.getStatus() != GameMarketOutcome.STATUS_ACTIVE){
                        logger.error("PreMatch Multiple can not cash out because market or outcome status is bad, selection={} {}", JSON.toJSONString(_selection), JSON.toJSONString(request));
                        response.getData().setMin(0.0);
                        response.getData().setMax(0.0);
                        return;
                    }

                    odds *= _selection.getOdd();
                }

            }

            if (odds == 1.0){
                logger.error("PreMatch can not cash out because not found odds {}", JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }
        }

        if (ticket.getRequest().getBetType() == BaseBetRequest.BET_TYPE_SINGLE){
            BetSelection selection = ticket.getSelection(request.getGameId(), request.getMarketId(), request.getSpecifiers(), request.getOutcomeId());
            if (selection == null || selection.getStatus() != BetSelection.STATUS_NOT_SETTLEMENT){
                logger.error("can not cash out because not found selection from ticket or selection status is bad {}", JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }

            response.getData().setOldOdds(selection.getOdd());
            response.getData().setNewOdds(odds);
        }else {
            if (!ticket.allSelectionsNotSettled()){
                logger.error("can not cash out because selection status is bad {}", JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }

            response.getData().setOldOdds(ticket.Odds());
            response.getData().setNewOdds(odds);
        }

        double min = 15;
        double max;
        double factor = 0.85;

        synchronized (ticket){
            if (ticket.getRequest().getBetType() == BaseBetRequest.BET_TYPE_SINGLE){
                // Single
                BetSelection selection = ticket.getSelection(request.getGameId(), request.getMarketId(), request.getSpecifiers(), request.getOutcomeId());

                if (selection == null || selection.getStatus() != BetSelection.STATUS_NOT_SETTLEMENT){
                    logger.error("can not cash out because not found selection from ticket or selection status is bad {}", JSON.toJSONString(request));
                    response.getData().setMin(0.0);
                    response.getData().setMax(0.0);
                    return;
                }

                // cashout = 过去的投注额 * 过去的赔率 / 新的赔率
                max = selection.getStake() * selection.getOdd() * factor / odds;
            }else {
                if (!ticket.allSelectionsNotSettled()){
                    logger.error("can not cash out because selection status is bad {}", JSON.toJSONString(request));
                    response.getData().setMin(0.0);
                    response.getData().setMax(0.0);
                    return;
                }

                max = ticket.getStake() * ticket.Odds() * factor / odds;
            }

            if (max < min){
                logger.error("can not cash out because max {} < min {} {}", max, min, JSON.toJSONString(request));
                response.getData().setMin(0.0);
                response.getData().setMax(0.0);
                return;
            }
        }

        response.getData().setMin(min);
        response.getData().setMax(max);
    }

    public void cashOut(CashOutRequest request, CashOutResponse response){
        CashOutRangeRequest cashOutRangeRequest = new CashOutRangeRequest();
        cashOutRangeRequest.setTicketId(request.getTicketId());
        cashOutRangeRequest.setGameId(request.getGameId());
        cashOutRangeRequest.setMarketId(request.getMarketId());
        cashOutRangeRequest.setSpecifiers(request.getSpecifiers());
        cashOutRangeRequest.setOutcomeId(request.getOutcomeId());

        CashOutRangeResponse cashOutRangeResponse = new CashOutRangeResponse();
        cashOutRangeResponse.setData(new CashOutRangeResponseData());

        cashOutRange(cashOutRangeRequest, cashOutRangeResponse);

        if (cashOutRangeResponse.getData().getMin() == 0){
            logger.error("can not cash out because some of selections disabled {}", JSON.toJSONString(request));
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg("Can not cash out because of selections disabled");
            return;
        }

        if (request.isFull()){
            logger.info("update cashout from {} to {}", request.getCashOut(), cashOutRangeResponse.getData().getMax());
            request.setCashOut(cashOutRangeResponse.getData().getMax());
        }

        if (request.getCashOut() < cashOutRangeResponse.getData().getMin() || request.getCashOut() > cashOutRangeResponse.getData().getMax()){
            logger.error("can not cash out because {} is not in the range {} {} {}", request.getCashOut(), cashOutRangeResponse.getData().getMin(), cashOutRangeResponse.getData().getMax(), JSON.toJSONString(request));
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg("Can not cash out because of the cashout is not in the range");
            return;
        }

        TicketManageUnit ticket = ticketFacade.getTicket(request.getTicketId());
        if (ticket == null){
            logger.error("can not cash out because not found ticket {}", JSON.toJSONString(request));
            response.setCode(Constants.CODE_APP_DISABLED);
            response.setMsg("Can not cash out because not found ticket");
            return;
        }

        logger.info("{} cash out newodds={} oldodds={}", request.getTicketId(), cashOutRangeResponse.getData().getNewOdds(), cashOutRangeResponse.getData().getOldOdds());

        double factor = 0.85;
        double betAmountDec = 0;
        double addAmount = request.getCashOut();

        synchronized (ticket){
            if (ticket.getRequest().getBetType() == BaseBetRequest.BET_TYPE_SINGLE){
                BetSelection selection = ticket.getSelection(request.getGameId(), request.getMarketId(), request.getSpecifiers(), request.getOutcomeId());
                if (selection == null || selection.getStatus() != BetSelection.STATUS_NOT_SETTLEMENT){
                    logger.error("Single can not cash out because some of selections disabled {}", JSON.toJSONString(request));
                    response.setCode(Constants.CODE_APP_DISABLED);
                    response.setMsg("Can not cash out because of selections disabled");
                    return;
                }

                double remainStake = selection.getCashOut().getRemainStake();

                /**
                 * 赔率不变的情况下
                 *
                 * 继续投注额 = 投注额-（提现额／系数）
                 *
                 *
                 * 赔率变化的情况下（赔率只会变大，变小的话会不允许部分cash out）
                 *
                 * 继续投注额 = 投注额-（提现额／系数 ）* 变化后赔率／变化前赔率
                 */

                if (request.isFull()){
                    betAmountDec = remainStake;
                }else {
                    if (cashOutRangeResponse.getData().getNewOdds() == cashOutRangeResponse.getData().getOldOdds()){
                        betAmountDec = request.getCashOut() / factor;
                        logger.info("{} odds not changed, cash out={} betAmountDec={}", request.getTicketId(), request.getCashOut(), betAmountDec);
                    }else if (cashOutRangeResponse.getData().getNewOdds() < cashOutRangeResponse.getData().getOldOdds()){
                        if (request.getCashOut() != cashOutRangeResponse.getData().getMax()){
                            logger.error("Single can not cash out because cashout != max when odds became smaller {}", JSON.toJSONString(request));
                            response.setCode(Constants.CODE_APP_DISABLED);
                            response.setMsg("Can not cash out because cashout != max when odds becaome smaller");
                            return;
                        }

                        betAmountDec = remainStake;
                    }else{
                        betAmountDec = (request.getCashOut() / factor) * cashOutRangeResponse.getData().getNewOdds() / cashOutRangeResponse.getData().getOldOdds();
                    }
                }

                remainStake -= betAmountDec;

                if (remainStake < 0){
                    logger.error("Single can not cash out because remain stake < 0 {}", JSON.toJSONString(request));
                    response.setCode(Constants.CODE_APP_DISABLED);
                    response.setMsg("Can not cash out because remain stake is too little");
                    return;
                }

                BetSelection _selection;
                if (ticket.getIsLive() == TicketManageUnit.LIVE_MATCH_BET){
                    _selection = liveGameFacade.getSelection(request.getGameId(), request.getMarketId(), request.getSpecifiers(), request.getOutcomeId());
                }else {
                    _selection = gameFacade.getSelection(request.getGameId(), request.getMarketId(), request.getSpecifiers(), request.getOutcomeId());
                }

                if (_selection == null){
                    logger.error("Single can not cash out because not found selection from match {}", JSON.toJSONString(request));
                    response.setCode(Constants.CODE_APP_DISABLED);
                    response.setMsg("Can not cash out because of selections disabled");
                    return;
                }

                double totalCashOut = request.getCashOut() + selection.getCashOut().getTotalCashOut();
                selection.getCashOut().setTotalCashOut(totalCashOut);
                selection.getCashOut().setRemainStake(remainStake);
                selection.setOdd(_selection.getOdd());

                ticketFacade.updateTicketCashOut(ticket);

                SportCashOutResponse sportCashOutResponse = accountFacade.sportCashOut(request.getUid(), betAmountDec, ticket.getTicketId(), addAmount, ticket.getTicketId());
                if (sportCashOutResponse == null || sportCashOutResponse.getCode() != 0){
                    logger.error("Single can not cash out because internal server error {}", JSON.toJSONString(request));
                    response.setCode(Constants.CODE_APP_DISABLED);
                    response.setMsg("Can not cash out because internal server error");
                    return;
                }
            }else {
                if (!ticket.allSelectionsNotSettled()){
                    logger.error("Multiple can not cash out because some of selections disabled {}", JSON.toJSONString(request));
                    response.setCode(Constants.CODE_APP_DISABLED);
                    response.setMsg("Can not cash out because of selections disabled");
                    return;
                }

                for (BetSelection selection : ticket.getRequest().getSelections()){
                    BetSelection _selection;
                    if (ticket.getIsLive() == TicketManageUnit.LIVE_MATCH_BET){
                        _selection = liveGameFacade.getSelection(selection.getGameId(), selection.getMarketId(), selection.getSpecifiers(), selection.getOutcomeId());
                    }else {
                        _selection = gameFacade.getSelection(selection.getGameId(), selection.getMarketId(), selection.getSpecifiers(), selection.getOutcomeId());
                    }

                    if (_selection == null){
                        logger.error("Multiple can not cash out because not found selection from match {}", JSON.toJSONString(request));
                        response.setCode(Constants.CODE_APP_DISABLED);
                        response.setMsg("Can not cash out because of selections disabled");
                        return;
                    }

                    selection.setOdd(_selection.getOdd());
                }

                double remainStake = ticket.getCashOut().getRemainStake();

                logger.info("{} cash out remain stake1 {}", request.getTicketId(), remainStake);
                /**
                 * 赔率不变的情况下
                 *
                 * 继续投注额 = 投注额-（提现额／系数）
                 *
                 *
                 * 赔率变化的情况下（赔率只会变大，变小的话会不允许部分cash out）
                 *
                 * 继续投注额 = 投注额-（提现额／系数 ）* 变化后赔率／变化前赔率
                 */
                if (request.isFull()){
                    betAmountDec = remainStake;
                }else {
                    if (cashOutRangeResponse.getData().getNewOdds() == cashOutRangeResponse.getData().getOldOdds()){
                        betAmountDec = request.getCashOut() / factor;
                        logger.info("{} odds not changed, cash out={} betAmountDec={}", request.getTicketId(), request.getCashOut(), betAmountDec);
                    }else if (cashOutRangeResponse.getData().getNewOdds() < cashOutRangeResponse.getData().getOldOdds()){
                        if (request.getCashOut() != cashOutRangeResponse.getData().getMax()){
                            logger.error("Multiple can not cash out because cashout != max when odds became smaller {}", JSON.toJSONString(request));
                            response.setCode(Constants.CODE_APP_DISABLED);
                            response.setMsg("Can not cash out because cashout != max when odds becaome smaller");
                            return;
                        }

                        betAmountDec = remainStake;
                    }else {
                        betAmountDec = (request.getCashOut() / factor) * cashOutRangeResponse.getData().getNewOdds() / cashOutRangeResponse.getData().getOldOdds();
                    }
                }

                remainStake -= betAmountDec;

                logger.info("{} cash out remain stake2 {}", request.getTicketId(), remainStake);

                if (remainStake < 0){
                    logger.error("Multiple can not cash out because remain stake < 0 {}", JSON.toJSONString(request));
                    response.setCode(Constants.CODE_APP_DISABLED);
                    response.setMsg("Can not cash out because remain stake is too little");
                    return;
                }

                double totalCashOut = request.getCashOut() + ticket.getCashOut().getTotalCashOut();
                ticket.getCashOut().setTotalCashOut(totalCashOut);
                ticket.getCashOut().setRemainStake(remainStake);

                ticketFacade.updateTicketCashOut(ticket);

                SportCashOutResponse sportCashOutResponse = accountFacade.sportCashOut(request.getUid(), betAmountDec, ticket.getTicketId(), addAmount, ticket.getTicketId());
                if (sportCashOutResponse == null || sportCashOutResponse.getCode() != 0){
                    logger.error("Single can not cash out because internal server error {}", JSON.toJSONString(request));
                    response.setCode(Constants.CODE_APP_DISABLED);
                    response.setMsg("Can not cash out because internal server error");
                    return;
                }
            }
        }

        // FIXME: 为了避免并发操作ticket，从数据库再查询一次, 也可以给ticket实现一个clone方法，但是太复杂了。
        ticket = mongoTemplate.findOne(new Query(Criteria.where("ticket_id").is(ticket.getTicketId())), TicketManageUnit.class, Constants.MONGO_COLLECTION_NAME_TICKET_MANAGE_UNITS);
        if (ticket != null){
            response.getData().setTicket(ticket);
        }
        response.setCode(Constants.CODE_SUCCESSFUL);
    }
}
