package com.stylefeng.guns.gateway.modular.film;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import com.stylefeng.guns.api.film.FilmAsyncServiceApi;
import com.stylefeng.guns.api.film.FilmServiceApi;
import com.stylefeng.guns.api.film.vo.*;
import com.stylefeng.guns.core.util.ToolUtil;
import com.stylefeng.guns.gateway.modular.film.vo.FilmConditionVO;
import com.stylefeng.guns.gateway.modular.film.vo.FilmIndexVO;
import com.stylefeng.guns.gateway.modular.film.vo.FilmRequestVO;
import com.stylefeng.guns.gateway.modular.vo.ResponseVo;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author: zhuanglj
 * @create: 2019-01-16 17:12
 */
@RestController
@RequestMapping("/film/")
public class FilmController {

    private static final String IMG_PRE = "http://img.meetingshop.cn/";

    @Reference(interfaceClass = FilmServiceApi.class)
    private FilmServiceApi filmServiceApi;

    @Reference(interfaceClass = FilmAsyncServiceApi.class, async = true)
    private FilmAsyncServiceApi filmAsyncServiceApi;

    /**
     * 获取信息接口
     * <p>
     * API网关：
     * 1. 功能聚合【API聚合】
     * 好处：
     * 1. 六个接口，一次请求，同一时刻节省了五次HTTP请求
     * 2. 同一个接口对外暴露，降低了前后端分离开发的难度和复杂度
     * 坏处：
     * 1. 一次获取数据过多，容易出现问题
     *
     * @return
     */
    @RequestMapping(value = "getIndex", method = RequestMethod.GET)
    public ResponseVo getIndex() {
        FilmIndexVO filmIndexVO = new FilmIndexVO();
        // 获取banner信息
        filmIndexVO.setBanners(filmServiceApi.getBanners());

        // 获取正在热映的电影
        filmIndexVO.setHotFilms(filmServiceApi.getHotFilms(true, 8, 1, 99, 99, 99, 99));

        // 获取即将上映的电影
        filmIndexVO.setSoonFilms(filmServiceApi.getSoonFilms(true, 8, 1, 99, 99, 99, 99));

        // 获取票房排行榜
        filmIndexVO.setBoxRanking(filmServiceApi.getBoxRanking());

        // 获取受欢迎的榜单
        filmIndexVO.setExpectRanking(filmServiceApi.getExpectRanking());

        // 获取前一百
        filmIndexVO.setTop100(filmServiceApi.getTop());

        return ResponseVo.success(IMG_PRE, filmIndexVO);
    }

    /**
     * 影片条件列表查询
     *
     * @param catId    :类型编号
     * @param sourceId :片源编号
     * @param yearId   :年代编号
     * @return
     */
    @RequestMapping(value = "getConditionList", method = RequestMethod.GET)
    public ResponseVo getConditionList(@RequestParam(name = "catId", required = false, defaultValue = "99") String catId,
                                       @RequestParam(name = "sourceId", required = false, defaultValue = "99") String sourceId,
                                       @RequestParam(name = "yearId", required = false, defaultValue = "99") String yearId) {
        // 标识位
        boolean flag = false;
        // 类型集合
        List<CatVO> cats = filmServiceApi.getCats();
        List<CatVO> catResult = new ArrayList<>();
        CatVO catVO = null;
        for (CatVO cat : cats) {
            // 判断集合是否存在catId，如果存在则将对应的实体变成active状态
            if ("99".equals(cat.getCatId())) {
                catVO = cat;
                continue;
            }
            if (cat.getCatId().equals(catId)) {
                flag = true;
                cat.setActive(true);
            } else {
                cat.setActive(false);
            }
            catResult.add(cat);
        }
        // 如果不存在，则默认将'全部'变为Active状态
        if (!flag && catVO != null) {
            catVO.setActive(true);
            catResult.add(catVO);
        } else {
            catVO.setActive(false);
            catResult.add(catVO);
        }

        // 片源集合
        flag = false;
        List<SourceVO> sources = filmServiceApi.getSources();
        List<SourceVO> sourceResult = new ArrayList<>();
        SourceVO sourceVO = null;
        for (SourceVO source : sources) {
            if ("99".equals(source.getSourceId())) {
                sourceVO = source;
                continue;
            }
            if (source.getSourceId().equals(sourceId)) {
                flag = true;
                source.setActive(true);
            } else {
                source.setActive(false);
            }
            sourceResult.add(source);
        }
        // 如果不存在，则默认将'全部'变为Active状态
        if (!flag && sourceVO != null) {
            sourceVO.setActive(true);
            sourceResult.add(sourceVO);
        } else {
            sourceVO.setActive(false);
            sourceResult.add(sourceVO);
        }

        // 年代集合
        flag = false;
        List<YearVO> years = filmServiceApi.getYears();
        List<YearVO> yearResult = new ArrayList<>();
        YearVO yearVO = null;
        for (YearVO year : years) {
            if ("99".equals(year.getYearId())) {
                yearVO = year;
                continue;
            }
            if (year.getYearId().equals(yearId)) {
                flag = true;
                year.setActive(true);
            } else {
                year.setActive(false);
            }
            yearResult.add(year);
        }
        // 如果不存在，则默认将'全部'变为Active状态
        if (!flag && yearVO != null) {
            yearVO.setActive(true);
            yearResult.add(yearVO);
        } else {
            yearVO.setActive(false);
            yearResult.add(yearVO);
        }

        FilmConditionVO filmConditionVO = new FilmConditionVO();
        filmConditionVO.setCatInfo(catResult);
        filmConditionVO.setSourceInfo(sourceResult);
        filmConditionVO.setYearInfo(yearResult);
        return ResponseVo.success(filmConditionVO);
    }

    @RequestMapping(value = "getFilms", method = RequestMethod.GET)
    public ResponseVo getFilms(FilmRequestVO filmRequestVO) {
        FilmVO filmVO;
        // 根据showType判断影片查询类型
        switch (filmRequestVO.getShowType()) {
            case 1:
                filmVO = filmServiceApi.getHotFilms(false, filmRequestVO.getPageSize(), filmRequestVO.getNowPage(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(), filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
            case 2:
                filmVO = filmServiceApi.getSoonFilms(false, filmRequestVO.getPageSize(), filmRequestVO.getNowPage(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(), filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
            case 3:
                filmVO = filmServiceApi.getClassicFilms(filmRequestVO.getPageSize(), filmRequestVO.getNowPage(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(), filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
            default:
                filmVO = filmServiceApi.getHotFilms(false, filmRequestVO.getPageSize(), filmRequestVO.getNowPage(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(), filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
        }
        // 根据sortId排序
        // 添加各种条件查询
        // 判断当前是第几页

        return ResponseVo.success(filmVO.getNowPage(), filmVO.getTotalPage(), IMG_PRE, filmVO.getFilmInfo());
    }

    @RequestMapping(value = "films/{searchParam}", method = RequestMethod.GET)
    public ResponseVo films(@PathVariable("searchParam") String searchParam,
                            int searchType) throws ExecutionException, InterruptedException {
        // 根据searchType判断查询类型
        // 不同的查询类型，传入的条件会略有不同
        FilmDetailVO filmDetail = filmServiceApi.getFilmDetail(searchType, searchParam);
        if (filmDetail == null) {
            return ResponseVo.serviceFail("没有可查询的影片");
        } else if (ToolUtil.isEmpty(filmDetail.getFilmId())) {
            return ResponseVo.serviceFail("没有可查询的影片");
        }
        String filmId = filmDetail.getFilmId();
        // 查询影片的详细信息 -> dubbo的异步调用
//        FilmDescVO filmDescVO = filmAsyncServiceApi.getFilmDesc(filmId);
        filmAsyncServiceApi.getFilmDesc(filmId);
        Future<FilmDescVO> filmDescVOFuture = RpcContext.getContext().getFuture();
        //获取图片信息
        filmAsyncServiceApi.getImg(filmId);
        Future<ImgVO> imgVOFuture = RpcContext.getContext().getFuture();

        //获取导演信息
        filmAsyncServiceApi.getDectInfo(filmId);
        Future<ActorVO> directorVOFuture = RpcContext.getContext().getFuture();

        //获取演员信息
        filmAsyncServiceApi.getActors(filmId);
        Future<List<ActorVO>> actorsFuture = RpcContext.getContext().getFuture();

        InfoRequestVO infoRequestVO = new InfoRequestVO();
        ActorRequestVO actorRequestVO = new ActorRequestVO();
        actorRequestVO.setActors(actorsFuture.get());
        actorRequestVO.setDirector(directorVOFuture.get());

        infoRequestVO.setActors(actorRequestVO);
        infoRequestVO.setBiography(filmDescVOFuture.get().getBiography());
        infoRequestVO.setImgs(imgVOFuture.get());
        infoRequestVO.setFilmId(filmId);
        filmDetail.setInfo04(infoRequestVO);
        return ResponseVo.success(IMG_PRE, filmDetail);
    }
}
