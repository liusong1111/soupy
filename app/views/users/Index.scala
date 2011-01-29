package views.users

import xml.Elem

class Index extends views.BaseView {
  def render = {


    <div>
      <script src=""></script>
      {
       <aa></aa>
      }
      {new ExchangeView}

      {new FeixinGroupView}
    </div>
  }
}

abstract class WidgetView extends views.BaseView {
  def render = {
    <div class="widget">
      <h3>{title}</h3>
      <div>
        {body}
      </div>
    </div>
  }

  def title:String

  def body: Elem
}

//积分兑换
class ExchangeView extends WidgetView {
  val title = "您可以使用积分兑换"

  def body = {
    <div>
      <div>
      周杰伦2011上海演唱会门票 6000积分
    </div>
      <div>
      周杰伦《七里香》专辑送高保真海报
3000积分
      </div>
    </div>
  }
}

//群组
class FeixinGroupView extends WidgetView {
  val title = "飞信群组"

  def body = {
    <div>
      <div>
      杰伦之家
    </div>
      <div>
      JAY
      </div>
    </div>
  }
}