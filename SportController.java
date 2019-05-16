package cc.gamemania.http.api.controller;

import cc.gamemania.http.api.proto.*;
import cc.gamemania.http.api.proto.entities.NotSettlement;
import cc.gamemania.http.api.proto.entities.RequestData;
import cc.gamemania.modules.RequestDispatcher;
import cc.gamemania.modules.account.proto.SendSMSRequest;
import cc.gamemania.modules.account.proto.SendSMSResponse;
import cc.gamemania.modules.ticket.api.proto.entities.BetSettlementRequest;
import cc.gamemania.modules.ticket.api.proto.entities.BetSettlementResponse;
import cc.gamemania.modules.ticket.api.proto.entities.UpdateGameStartTimeRequest;
import cc.gamemania.modules.ticket.api.proto.entities.UpdateGameStartTimeResponse;
import cc.gamemania.utils.Helper;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by pandazhong on 17/7/11.
 */

@Api(value = "Sport Service API", description = "Betting Service API")
@RestController
public class SportController {
    private org.slf4j.Logger logger = LoggerFactory.getLogger("BetController");

    @Autowired
    private RequestDispatcher requestDispatcher;

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询运动分类信息", httpMethod = "POST", response = SportCategoriesResponse.class, notes = "查询运动问类信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportCategoriesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/sports", method = RequestMethod.POST)
    public SportCategoriesResponse getSports(@RequestBody SportCategoriesRequest request) {
        //return requestDispatcher.sportCategories(request);

        SportCategoriesResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/sports " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.sportCategories(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询指定运动分类的国家信息", httpMethod = "POST", response = SportCountriesResponse.class, notes = "查询指定运动分类的国家信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportCountriesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/sport/countries", method = RequestMethod.POST)
    public SportCountriesResponse getSportCountries(@RequestBody SportCountriesRequest request) {
        //return requestDispatcher.sportCountries(request);

        SportCountriesResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/sport/countries " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.sportCountries(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询指定运动分类的国家信息", httpMethod = "POST", response = SportCountriesResponse.class, notes = "查询指定运动分类的国家信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportCountriesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v2/sport/countries", method = RequestMethod.POST)
    public SportCountriesResponse getSportCountriesV2(@RequestBody SportCountriesRequest request) {
        //return requestDispatcher.sportCountries(request);

        SportCountriesResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v2/sport/countries " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.sportCountriesV2(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 查询指定运动分类的国家信息", httpMethod = "POST", response = SportCountriesResponse.class, notes = "LIVE 查询指定运动分类的国家信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportCountriesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/live/sport/countries", method = RequestMethod.POST)
    public SportCountriesResponse getLiveSportCountries(@RequestBody SportCountriesRequest request) {
        //return requestDispatcher.sportCountries(request);

        SportCountriesResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/live/sport/countries " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveSportCountries(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 查询指定运动分类的国家信息", httpMethod = "POST", response = SportCountriesResponse.class, notes = "LIVE 查询指定运动分类的国家信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportCountriesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v2/live/sport/countries", method = RequestMethod.POST)
    public SportCountriesResponse getLiveSportCountriesV2(@RequestBody SportCountriesRequest request) {
        //return requestDispatcher.sportCountries(request);

        SportCountriesResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v2/live/sport/countries " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveSportCountriesV2(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询指定运动分类的联赛信息", httpMethod = "POST", response = TournamentsResponse.class, notes = "查询指定运动分类的联赛信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "TournamentsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/tournaments", method = RequestMethod.POST)
    public TournamentsResponse getSportCountryTournaments(@RequestBody TournamentsRequest request) {
        //return requestDispatcher.tournaments(request);

        TournamentsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/tournaments " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.tournaments(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 查询指定运动分类的联赛信息", httpMethod = "POST", response = TournamentsResponse.class, notes = "LIVE 查询指定运动分类的联赛信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "TournamentsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/live/tournaments", method = RequestMethod.POST)
    public TournamentsResponse getLiveSportCountryTournaments(@RequestBody TournamentsRequest request) {
        TournamentsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/live/tournaments " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveTournaments(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询指定运动分类的相关比赛(GameEntity)以及其默认赔率(Odds)信息", httpMethod = "POST", response = MatchesOddsResponse.class, notes = "查询指定运动分类的相关比赛(GameEntity)以及其默认赔率(Odds)信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MatchesOddsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/matches/odds", method = RequestMethod.POST)
    public MatchesOddsResponse getGameMarketLines(@RequestBody MatchesOddsRequest request) {
        //return requestDispatcher.matchesOdds(request);

        MatchesOddsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/matches/odds " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.matchesOdds(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 查询指定运动分类的相关比赛(GameEntity)以及其默认赔率(Odds)信息", httpMethod = "POST", response = MatchesOddsResponse.class, notes = "LIVE 查询指定运动分类的相关比赛(GameEntity)以及其默认赔率(Odds)信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MatchesOddsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/live/matches/odds", method = RequestMethod.POST)
    public MatchesOddsResponse getLiveGameMarketLines(@RequestBody MatchesOddsRequest request) {
        //return requestDispatcher.matchesOdds(request);

        MatchesOddsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/live/matches/odds " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveMatchesOdds(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询指定比赛的一组下注方式(Market)", httpMethod = "POST", response = MatchMarketsResponse.class, notes = "获取指定比赛的markets")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MatchMarketsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/match/markets", method = RequestMethod.POST)
    public MatchMarketsResponse matchMarkets(@RequestBody MatchMarketsRequest request) {
        //return requestDispatcher.matchMarkets(request);

        MatchMarketsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/match/markets " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.matchMarkets(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} cost {}ms, response: {}", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs(), JSON.toJSONString(response));

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 查询指定比赛的一组下注方式(Market)", httpMethod = "POST", response = MatchMarketsResponse.class, notes = "LIVE 获取指定比赛的markets")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MatchMarketsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/live/match/markets", method = RequestMethod.POST)
    public MatchMarketsResponse liveMatchMarkets(@RequestBody MatchMarketsRequest request) {
        MatchMarketsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/live/match/markets " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveMatchMarkets(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取热门比赛", httpMethod = "POST", response = HotMatchesResponse.class, notes = "获取热门比赛")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "HotMatchesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/hot/matches", method = RequestMethod.POST)
    public HotMatchesResponse hotMatches(@RequestBody HotMatchesRequest request) {
        //return requestDispatcher.hotMatches(request);

        HotMatchesResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/hot/matches " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.hotMatches(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取比赛详情", httpMethod = "POST", response = MatchInfoResponse.class, notes = "获取比赛详情")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MatchInfoRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/match/info", method = RequestMethod.POST)
    public MatchInfoResponse matchInfo(@RequestBody MatchInfoRequest request) {
        //return requestDispatcher.matchInfo(request);

        MatchInfoResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/match/info " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.matchInfo(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 获取比赛详情", httpMethod = "POST", response = MatchInfoResponse.class, notes = "LIVE 获取比赛详情")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MatchInfoRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/live/match/info", method = RequestMethod.POST)
    public MatchInfoResponse liveMatchInfo(@RequestBody MatchInfoRequest request) {
        MatchInfoResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/live/match/info " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveMatchInfo(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "请求Single下注", httpMethod = "POST", response = SingleBetResponse.class, notes = "请求Single下注")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SingleBetRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/bet/slip/single", method = RequestMethod.POST)
    public SingleBetResponse betSlipSingle(HttpServletRequest r, @RequestBody SingleBetRequest request) {
        String clientIp = r.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp == "") {
            clientIp = r.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp == "") {
            clientIp = r.getRemoteHost();
        }
        if (request != null) {
            request.setClientIp(clientIp);
        }

        //return requestDispatcher.singleBet(request);

        SingleBetResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/bet/slip/single " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.singleBet(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 请求Single下注", httpMethod = "POST", response = SingleBetResponse.class, notes = "LIVE 请求Single下注")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SingleBetRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/live/bet/slip/single", method = RequestMethod.POST)
    public SingleBetResponse liveBetSlipSingle(HttpServletRequest r, @RequestBody SingleBetRequest request) {
        String clientIp = r.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp == "") {
            clientIp = r.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp == "") {
            clientIp = r.getRemoteHost();
        }
        if (request != null) {
            request.setClientIp(clientIp);
        }

        SingleBetResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/live/bet/slip/single " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveSingleBet(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "请求Multiple下注", httpMethod = "POST", response = MultipleBetResponse.class, notes = "请求Multiple下注")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MultipleBetRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/bet/slip/multiple", method = RequestMethod.POST)
    public MultipleBetResponse betSlipMultiple(HttpServletRequest r, @RequestBody MultipleBetRequest request) {
        String clientIp = r.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp == "") {
            clientIp = r.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp == "") {
            clientIp = r.getRemoteHost();
        }
        if (request != null) {
            request.setClientIp(clientIp);
        }

        //return requestDispatcher.multipleBet(request);

        MultipleBetResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/bet/slip/multiple " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.multipleBet(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "LIVE 请求Multiple下注", httpMethod = "POST", response = MultipleBetResponse.class, notes = "LIVE 请求Multiple下注")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MultipleBetRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/live/bet/slip/multiple", method = RequestMethod.POST)
    public MultipleBetResponse liveBetSlipMultiple(HttpServletRequest r, @RequestBody MultipleBetRequest request) {
        String clientIp = r.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp == "") {
            clientIp = r.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp == "") {
            clientIp = r.getRemoteHost();
        }
        if (request != null) {
            request.setClientIp(clientIp);
        }

        MultipleBetResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/live/bet/slip/multiple " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.liveMultipleBet(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "请求Double下注", httpMethod = "POST", response = DoubleBetResponse.class, notes = "请求Double下注")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "DoubleBetRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/bet/slip/double", method = RequestMethod.POST)
    public DoubleBetResponse betSlipDouble(HttpServletRequest r, @RequestBody DoubleBetRequest request) {
        String clientIp = r.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp == "") {
            clientIp = r.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp == "") {
            clientIp = r.getRemoteHost();
        }
        if (request != null) {
            request.setClientIp(clientIp);
        }

        DoubleBetResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/bet/slip/double " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.doubleBet(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "请求获取历史下注Ticket", httpMethod = "POST", response = HistoryTicketsResponse.class, notes = "请求获取历史下注Ticket")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "HistoryTicketsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/history/tickets", method = RequestMethod.POST)
    public HistoryTicketsResponse historyTickets(HttpServletRequest r, @RequestBody HistoryTicketsRequest request) {
        //return requestDispatcher.historyTickets(request);

        HistoryTicketsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/history/tickets " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.historyTickets(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "请求获取历史下注Ticket(管理员)", httpMethod = "POST", response = HistoryTicketsResponse.class, notes = "请求获取历史下注Ticket(管理员)")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "HistoryTicketsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/history/tickets/admin", method = RequestMethod.POST)
    public HistoryTicketsResponse historyTicketsAdmin(HttpServletRequest r, @RequestBody HistoryTicketsRequest request) {
        //return requestDispatcher.historyTicketsAdmin(request);

        HistoryTicketsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/history/tickets/admin " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.historyTicketsAdmin(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取指定运动的一段时间内的所有比赛", httpMethod = "POST", response = GamesResponse.class, notes = "获取指定运动的一段时间内的所有比赛")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "GamesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/games", method = RequestMethod.POST)
    public GamesResponse games(@RequestBody GamesRequest request) {
        //return requestDispatcher.games(request);

        GamesResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/games " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.games(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "更新指定比赛的内容", httpMethod = "POST", response = UpdateGameResponse.class, notes = "更新指定比赛的内容")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "UpdateGameRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/update/game", method = RequestMethod.POST)
    public UpdateGameResponse updateGame(@RequestBody UpdateGameRequest request) {
        //return requestDispatcher.updateGame(request);

        UpdateGameResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/update/game " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.updateGame(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询Ticket相关事件状态", httpMethod = "POST", response = TicketEventResponse.class, notes = "查询Ticket相关事件状态")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "TicketEventRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/ticket/event", method = RequestMethod.POST)
    public TicketEventResponse ticketEvent(@RequestBody TicketEventRequest request) {
        //return requestDispatcher.ticketEvent(request);

        TicketEventResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/ticket/event " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.ticketEvent(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "恢复事件赔率", httpMethod = "POST", response = RecoveryOddsResponse.class, notes = "恢复事件赔率")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "RecoveryOddsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/recovery/odds", method = RequestMethod.POST)
    public RecoveryOddsResponse recoveryOdds(@RequestBody RecoveryOddsRequest request) {
        RecoveryOddsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/recovery/odds " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.recoveryOdds(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "恢复事件状态", httpMethod = "POST", response = RecoveryStatefulResponse.class, notes = "恢复事件状态")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "RecoveryStatefulRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/recovery/stateful", method = RequestMethod.POST)
    public RecoveryStatefulResponse recoveryStateful(@RequestBody RecoveryStatefulRequest request) {
        RecoveryStatefulResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/recovery/stateful " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.recoveryStateful(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询比赛状态", httpMethod = "POST", response = MatchStatusResponse.class, notes = "查询比赛状态")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "MatchStatusRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/match/status", method = RequestMethod.POST)
    public MatchStatusResponse matchStatus(@RequestBody MatchStatusRequest request) {
        MatchStatusResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/match/status " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.matchStatus(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "测试推送消息", httpMethod = "POST", response = TestPushMessageResponse.class, notes = "测试推送消息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "TestPushMessageRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/test/push/message", method = RequestMethod.POST)
    public TestPushMessageResponse testPushMessage(@RequestBody TestPushMessageRequest request) {
        TestPushMessageResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/test/push/message " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.testPushMessage(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取Ticket详情", httpMethod = "POST", response = TicketDetailResponse.class, notes = "获取Ticket详情")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "TicketDetailRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/ticket/detail", method = RequestMethod.POST)
    public TicketDetailResponse ticketDetail(@RequestBody TicketDetailRequest request) {
        TicketDetailResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/ticket/detail " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.ticketDetail(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "测试发送SMS", httpMethod = "POST", response = SendSMSResponse.class, notes = "测试发送SMS")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SendSMSRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/send/sms", method = RequestMethod.POST)
    public SendSMSResponse ticketDetail(@RequestBody SendSMSRequest request) {
        SendSMSResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/send/sms " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.sendSMS(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "比赛延迟报告", httpMethod = "POST", response = GameDelayReportResponse.class, notes = "比赛延迟报告")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "GameDelayReportRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/game/delay/report", method = RequestMethod.POST)
    public GameDelayReportResponse gameDelayReport(@RequestBody GameDelayReportRequest request) {
        GameDelayReportResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/game/delay/report " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.gameDelayReport(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "订阅LIVE比赛推送消息", httpMethod = "POST", response = SubscribeGameResponse.class, notes = "订阅LIVE比赛推送消息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SubscribeGameRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/subscribe/game", method = RequestMethod.POST)
    public SubscribeGameResponse subscribeGame(@RequestBody SubscribeGameRequest request) {
        SubscribeGameResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/subscribe/game " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.subscribeGame(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "取消订阅LIVE比赛推送消息", httpMethod = "POST", response = UnsubscribeGameResponse.class, notes = "取消订阅LIVE比赛推送消息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "UnsubscribeGameRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/unsubscribe/game", method = RequestMethod.POST)
    public UnsubscribeGameResponse unsubscribeGame(@RequestBody UnsubscribeGameRequest request) {
        UnsubscribeGameResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/unsubscribe/game " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.unsubscribeGame(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "测试结算", httpMethod = "POST", response = SportWinResponse.class, notes = "测试结算")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportWinRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/sport/win/test", method = RequestMethod.POST)
    public SportWinResponse sportWin(@RequestBody SportWinRequest request) {
        SportWinResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/sport/win/test " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.sportWin(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "测试退款", httpMethod = "POST", response = SportRefundResponse.class, notes = "测试退款")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportRefundRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/sport/refund/test", method = RequestMethod.POST)
    public SportRefundResponse sportRefund(@RequestBody SportRefundRequest request) {
        SportRefundResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/sport/refund/test " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.sportRefund(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "手动结算Multiple", httpMethod = "POST", response = ManualMultipleBetSettlementResponse.class, notes = "手动结算Multiple")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "ManualMultipleBetSettlementRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/manual/multiple/bet/settlement", method = RequestMethod.POST)
    public ManualMultipleBetSettlementResponse manualMultipleBetSettlement(@RequestBody ManualMultipleBetSettlementRequest request) {
        ManualMultipleBetSettlementResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/manual/multiple/bet/settlement " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.manualMultipleBetSettlement(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "手动结算Single", httpMethod = "POST", response = ManualSingleBetSettlementResponse.class, notes = "手动结算Single")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "ManualSingleBetSettlementRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/manual/single/bet/settlement", method = RequestMethod.POST)
    public ManualSingleBetSettlementResponse manualSingleBetSettlement(@RequestBody ManualSingleBetSettlementRequest request) {
        ManualSingleBetSettlementResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/manual/single/bet/settlement " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.manualSingleBetSettlement(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取未来LIVE比赛", httpMethod = "POST", response = FutureLiveCompetitionsResponse.class, notes = "获取未来LIVE比赛")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "FutureLiveCompetitionsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/future/live/competitions", method = RequestMethod.POST)
    public FutureLiveCompetitionsResponse futureLiveCompetitions(@RequestBody FutureLiveCompetitionsRequest request) {
        FutureLiveCompetitionsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/future/live/competitions " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.futureLiveCompetitions(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} cost {}ms", requestData.getRequestId(), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "设置一组推荐的比赛", httpMethod = "POST", response = RecommandCompetitionsResponse.class, notes = "设置一组推荐的比赛")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "RecommandCompetitionsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/recommand/competitions", method = RequestMethod.POST)
    public RecommandCompetitionsResponse recommandCompetitions(@RequestBody RecommandCompetitionsRequest request) {
        RecommandCompetitionsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/recommand/competitions " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.recommandCompetitions(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} {} cost {}ms", requestData.getRequestId(), JSON.toJSONString(request), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取一组比赛以便查询结果", httpMethod = "POST", response = CompetitionsWithResultsResponse.class, notes = "获取一组比赛以便查询结果")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "CompetitionsWithResultsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/competitions/with/results", method = RequestMethod.POST)
    public CompetitionsWithResultsResponse competitionsWithResults(@RequestBody CompetitionsWithResultsRequest request) {
        CompetitionsWithResultsResponse response = null;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/competitions/with/results " + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.competitionsWithResults(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);
        logger.info("The request {} {} cost {}ms", requestData.getRequestId(), JSON.toJSONString(request), requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     * 获取未结算的信息给 betika
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取所有未结算的信息", httpMethod = "POST", response = NotSettlementResponse.class, notes = "获取所有未结算的信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "NotSettlementRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/not/settlement/games", method = RequestMethod.POST)
    public NotSettlementResponse<NotSettlement> notSettlement(@RequestBody NotSettlementRequest request) {
        NotSettlementResponse<NotSettlement> response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/not/settlement/games" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());
        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.notSettlement(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }


    /**
     * settlement by the betika
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "betika来结算", httpMethod = "POST", response = BaseResponse.class, notes = "betika来结算")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "BetikaSettlementRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/betika/settlement", method = RequestMethod.POST)
    public BaseResponse betikaSettlement(@RequestBody BetikaSettlementRequest request) {
        BaseResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/betika/settlement" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.betikaSettlement(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     * call by mts gateway
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "mts gateway通知投注结果", httpMethod = "POST", response = TicketResultResponse.class, notes = "mts gateway通知投注结果")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "TicketResultRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/ticket/result/callback", method = RequestMethod.POST)
    public TicketResultResponse ticketResult(@RequestBody TicketResultRequest request) {
        TicketResultResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/ticket/result/callback" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.ticketResultCallback(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取season", httpMethod = "POST", response = SeasonResponse.class, notes = "获取season")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SeasonRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/season", method = RequestMethod.POST)
    public SeasonResponse season(@RequestBody SeasonRequest request) {
        SeasonResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/season" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.season(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "操作bet", httpMethod = "POST", response = BetCtlResponse.class, notes = "操作bet")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "BetCtlRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/bet/ctl", method = RequestMethod.POST)
    public BetCtlResponse betCtl(@RequestBody BetCtlRequest request) {
        BetCtlResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/bet/ctl" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.betCtl(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "查询赔率", httpMethod = "POST", response = FindOddsResponse.class, notes = "查询赔率")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "FindOddsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/find/odds", method = RequestMethod.POST)
    public FindOddsResponse findOdds(@RequestBody FindOddsRequest request) {
        FindOddsResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/find/odds" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.findOdds(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "手动设置赔率等信息", httpMethod = "POST", response = ManualOddsChangeResponse.class, notes = "手动设置赔率等信息")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "ManualOddsChangeRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/manual/odds/change", method = RequestMethod.POST)
    public ManualOddsChangeResponse manualOddsChange(@RequestBody ManualOddsChangeRequest request) {
        ManualOddsChangeResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/manual/odds/change" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.manualOddsChange(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "搜索比赛", httpMethod = "POST", response = SearchGamesResponse.class, notes = "搜索比赛")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SearchGamesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/search/games", method = RequestMethod.POST)
    public SearchGamesResponse searchGames(@RequestBody SearchGamesRequest request) {
        SearchGamesResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/search/games" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.searchGames(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "重新加载未结算订单", httpMethod = "POST", response = ReloadNotSettledTicketResponse.class, notes = "重新加载未结算订单")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "ReloadNotSettledTicketRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/reload-not-settled-ticket", method = RequestMethod.POST)
    public ReloadNotSettledTicketResponse reloadNotSettledTicket(@RequestBody ReloadNotSettledTicketRequest request) {
        ReloadNotSettledTicketResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/reload-not-settled-ticket" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.reloadNotSettledTicket(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "默认运动", httpMethod = "POST", response = DefaultSportResponse.class, notes = "默认运动")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "DefaultSportRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/default/sport", method = RequestMethod.POST)
    public DefaultSportResponse defaultSport(@RequestBody DefaultSportRequest request) {
        DefaultSportResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/default/sport" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.defaultSport(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "获取cashout范围", httpMethod = "POST", response = CashOutRangeResponse.class, notes = "获取cashout范围")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "CashOutRangeRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/cashout/range", method = RequestMethod.POST)
    public CashOutRangeResponse cashOutRange(@RequestBody CashOutRangeRequest request) {
        CashOutRangeResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/cashout/range" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.cashOutRange(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "Cashout", httpMethod = "POST", response = CashOutResponse.class, notes = "Cashout")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "CashOutRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/cashout", method = RequestMethod.POST)
    public CashOutResponse cashOut(@RequestBody CashOutRequest request) {
        CashOutResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/cashout" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.cashOut(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "BetSettlementResponse", httpMethod = "POST", response = BetSettlementResponse.class, notes = "结算例子")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "BetSettlementRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/bet-settlement-example", method = RequestMethod.POST)
    public BetSettlementResponse betSettlementExample(@RequestBody BetSettlementRequest request) {
        return new BetSettlementResponse();
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "UpdateGameStartTimeResponse", httpMethod = "POST", response = UpdateGameStartTimeResponse.class, notes = "更新比赛时间")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "UpdateGameStartTimeRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/update-game-start-time-example", method = RequestMethod.POST)
    public UpdateGameStartTimeResponse updateGameStartTimeExample(@RequestBody UpdateGameStartTimeRequest request) {
        return new UpdateGameStartTimeResponse();
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "QueryOddsResponse", httpMethod = "POST", response = QueryOddsResponse.class, notes = "查询赔率")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "QueryOddsRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/query/odds", method = RequestMethod.POST)
    public QueryOddsResponse queryOdds(@RequestBody QueryOddsRequest request) {
        QueryOddsResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/query/odds" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.queryOdds(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "SportsNumberResponse", httpMethod = "POST", response = SportsNumberResponse.class, notes = "比赛场次")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "SportsNumberRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/sports/number", method = RequestMethod.POST)
    public SportsNumberResponse sportsNumber(@RequestBody SportsNumberRequest request) {
        SportsNumberResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/sports/number" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.sportsNumber(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    @CrossOrigin(origins = "*")
    @ApiOperation(value = "ClearGamesResponse", httpMethod = "POST", response = ClearGamesResponse.class, notes = "清除比赛")
    @ApiImplicitParam(name = "request", value = "请求实体", required = true, dataType = "ClearGamesRequest")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "请求成功"),
            @ApiResponse(code = 400, message = "请求格式错误"),
            @ApiResponse(code = 403, message = "请求被拒绝执行"),
            @ApiResponse(code = 404, message = "找不到资源"),
            @ApiResponse(code = 500, message = "服务器内部错误")}
    )
    @RequestMapping(path = "/sports/v1/clear/games", method = RequestMethod.POST)
    public ClearGamesResponse clearGames(@RequestBody ClearGamesRequest request) {
        ClearGamesResponse response;

        RequestData requestData = new RequestData();
        requestData.setData(request);
        requestData.setRequestId("/sports/v1/clear/games" + Helper.generateRequestId());
        requestData.setTsBeginMs(System.currentTimeMillis());

        requestDispatcher.addRequest(requestData);

        response = requestDispatcher.clearGames(request);

        requestData.setTsEndMs(System.currentTimeMillis());
        requestDispatcher.removeRequest(requestData);

        logger.info("The request {} {} spent {}ms", requestData.getRequestId(), JSON.toJSONString(request),
                requestData.getTsEndMs() - requestData.getTsBeginMs());

        return response;
    }
}