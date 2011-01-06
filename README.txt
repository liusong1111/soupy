== soupy
a full-featured web framework powered by scala, highly inspired by ruby on rails3.
including:MVC-Routes, View-Composition, ORM.
support development,production,test mode.

== stage
early experimental stage.
only chunks of code in mass.

== modules
* MVC-Routes
  use rails3 style declaration, with compiler's check.
* View-Composition
  * view template engine: prototype-oriented programming
  * Arbitrary composition: component-based programming. for example, layout is a normal top level composition.
* ORM
  * design as DataMapper mode, with ActiveRecord3 API Style.
  * full-compiler & IDE utilities.

== why?
  Q: Why not just ruby on rails?
  A: Not static typed, no compile-phase check, performance scare, not java close friendly.
     scala is here instead of jruby, groovy.(groovy++ is promising, but too young)
  Q: Why not scalatra, lift, even playframework?
  A: All of them have things I dislike, and I want to create one from ground.
  Q: Why name as soupy?
  A: Thanks to jsoup library used in my first version.
  Q: Why no rack style layer,no framework-agnostic,no micro-core and plugin architecture, no CoC enough,...
  A: We are young, and we are not that clear. Expect your suggestion.

== Usage
  ooooooooops, see source code.

