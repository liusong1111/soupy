package controllers

import soupy.Controller
import views.BaseView
import views.users.{FeixinGroupView, ExchangeView, WidgetView}
import models._

class SearchPortalController extends Controller {
  def index = {
    val q = params("q")
    var list = List[SearchResultItem]()

    if (q == "NBA") {
      list = List(new SearchResultItem("全球通预存话费得NBA中国赛门票_中国移动通信",
        "活动期间，全球通客户本地预存或充值卡方式存入一定金额话费，然后通过短信渠道办理本业务，即可获赠相应档位的NBA中国赛北京站门票两张，门票数量有限，先到先得...",
        "10086.cn/whatsnew/styles/gddt/bj/aintro/2",
        "2010年1月21日"),
        new SearchResultItem("NBA活动",
          "2010年6月4日开始，活动时间与NBA总决赛直播时间相同。活动时间仅为有NBA总决赛日期。 参与方式 看NBA总决赛视频直播，将用户个人对比赛的评论内容发表在NBA视频...",
          "huodong.feixin.10086.cn/zhuanti/index.asp",
          "2010年1月21日"),
        new SearchResultItem("全球通推出NBA北京赛积分换票活动_中国移动通信",
          "2009年9月1日... 2009年度的NBA北京赛即将临近，这样尽情享受精彩赛事的机会不可多得。为此，赢取NBA北京赛的门票，亲临现场体验NBA运动的魅力，已成为",
          "10086.cn/aboutus/news/200909/t20090901_12",
          "2010年1月21日")
      )
    } else {
      list = List(new SearchResultItem("周杰伦《青蜂侠》发预告片",
        "周杰伦《青蜂侠》发预告片 . 摘要： 电影《青蜂侠》中，饰演武器功夫专家的周董在照片中一身黑衣黑帽黑眼罩，十分有型。但美国网友却认为周杰伦不适合演加藤， 而遭到不少网友的抨击。 周杰伦进军好莱坞影作《青蜂侠》剧照昨日正式曝光，饰演武器功夫专家的周董在照片中一身黑衣黑帽黑眼罩，十分有型。周杰伦将代替周星驰，在新版《青蜂侠》中饰演...",
        "http://music.10086.cn/newweb/news/newsshow/20100625/i/105446.html",
        "2010年1月21日"),
        new SearchResultItem("周杰伦-重庆奥体中心",
          "周杰伦-重庆奥体中心. 周杰伦用歌声来表达爱 重庆演唱会募捐305万2008年05月25日 13:24 来源：华龙网－重庆商报喊四川雄起-与巴蜀小学30位学生齐唱《蜗牛》，为灾区小朋友祈福-苏芮、周蕙、刘畊宏爱心助阵，“爱心贴”成独特风景。昨日20∶30，“抗震救灾周杰伦慈善募捐演唱会”在奥体中心举行。昨晚的演唱会上，虽然有苏芮、周蕙、刘畊宏和南拳妈妈等明星嘉宾助阵演出，还有...",
          "http://www.cq.10086.cn/mzone/column/music_gather/mx_1.html",
          "2010年1月21日"),
        new SearchResultItem("周杰伦北京演唱会",
          "周杰伦北京演唱会. 摘要： 接连不断的广告与影片合约，让周杰伦辗转半个地球开拓自己的娱乐版图，集歌手，制作人，导演于一身，但是无论他有多忙，终归要回归他的音乐，因为歌迷需要他，更需要在周杰伦北京演唱会上看到他！ 周杰伦演唱会海报 接连不断的广告与影片合约，让周杰伦辗转半个地球开拓自己的娱乐版图，集歌手，制作人，导演于一身，但是无论他有多忙...",
          "http://music.10086.cn/newweb/news/newsshow/20100505/i/100695.html",
          "2010年1月21日")
      )
    }

    var views = List[WidgetView](new ExchangeView, new FeixinGroupView, new SearchInformation(q))
    var b = new SearchSpecialPanel(views)
    var a = new SearchResultPanel("其它结果", list)
    var panel = new SearchResultPage(a, b)
    out.print(panel.render)
  }
}

class SearchInformation(q: String) extends WidgetView {
  def title = "139社区"

  def body = {
    val list = if(q eq null){Information.limit(10).all}else{Information.search(q).all}
    <div>
      {list.map {
      info => <div class="specialItem">
        <img src={"/images/ring/" + info.icon} style="width:32px;height:32px;" />
        {info.title}
        <a href={info.lpath}>查看</a>
      </div>
    }}
    </div>
  }
}


class SearchResultPage(var resultPanel: SearchResultPanel, var specialPanel: SearchSpecialPanel) extends BaseView {
  override def render = {
    <div class="searchResultPage">
      <script src="/javascripts/jquery-1.4.4.js"></script>
      <script src="/javascripts/search.js"></script>
      <div style="margin:6px;padding:6px;float:right;overflow:visible">
        <form action="/search">
            <input type="text" name="q"/>
          <button>搜索</button>
        </form>
      </div>
      <table>
        <tr>
          <td style="vertical-align:top">{resultPanel.render}</td>
          <td style="vertical-align:top">{specialPanel.render}</td>
        </tr>
      </table>
    </div>
  }
}

class SearchResultPanel(var title: String, var items: List[SearchResultItem]) extends BaseView {
  override def render = {
    <div class="searchResultPanel">
        <link rel="stylesheet" type="text/css" href="/stylesheets/search.css"/>
      <h3>{title}</h3>
      {items.map(_.render)}
    </div>
  }
}

class SearchResultItem(var title: String, var content: String, var url: String, var date: String) extends BaseView {
  override def render = {
    <div class="searchResultItem">
      <h4>{title}</h4>
      <div class="content">{content}</div>
      <a href={url}>{url}</a>
      <span class="date">{date}</span>
    </div>
  }
}

class SearchSpecialPanel(var views: List[WidgetView]) extends BaseView {
  override def render = {
    <div class="specialSearchPanel">
        <link rel="stylesheet" type="text/css" href="/stylesheets/widget.css"/>
      {views.map(_.render)}
    </div>
  }
}