   之前用过基于web版微信的机器人，但是有几个缺点：比如新号不能登录，经常被封等问题。app版的微信机器人原理是模拟人工点击，能避免web版的很多问题，缺点是速度比较慢，成本比较高。目前实现的功能是自动回复消息、主动发送消息、同意好友申请。 
先上个图感受一下： 

自动回复：![image](https://github.com/DaQinShgy/Robot/blob/master/sendMsg.gif)
同意好友申请：![image](https://github.com/DaQinShgy/Robot/blob/master/agreeAddFriend.gif)
主动发送消息：![image](https://github.com/DaQinShgy/Robot/blob/master/pushMsg.gif)
 
 1、准备工作：老版本微信，我用的是6.5.10，之所以不用最新版是因为聊天界面的文本区老版本是textview，新版本改成了view，导致无法拿到消息内容。 
    
   如果想输入汉字的话，还要准备一个特殊的输入法：ADBKeyBoard，普通的输入法是不支持的，安装包ADBKeyBoard.apk放在了项目里，在文章最后。 
    
   获取控件id我用的是monitor，在studio工具栏的后面，打开并连接手机成功之后，点击获取当前手机屏幕，选中你要的控件，在Node Detail里就可以看到它的相关信息了。 
   
   已获取root权限的手机一部
   
 2、开发：主要的技术点来源于github上大神做的抢红包外挂AccessibilityService和Shell命令，前者用于定位到具体的页面与控件还有模拟操作，后者主要用于模拟操作。AccessibilityService在做模拟操作有一定的缺陷，比如没有id的控件是无法点击的，点击次数不好控制，Shell命令则可以完美的实现这两点。
