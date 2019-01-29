 MimiDouyin项目总结

小组成员及分工
朱长昊1120171227
- 完成录制功能、
- 上传、获取信息功能
- 完成收藏功能
- 完善播放功能，添加动画

张佳明1120173305
- 完成定位功能
- 完成播放功能
- 制作展示ppt

李传赫1120172774
- UI设计
- 首页图设计制作
- 完成开屏页面

主要功能展示
播放功能
- 播放功能通过点击屏幕和底部的进度条来控制播放
- 此外我们在播放功能的基础上添加了收藏功能，添加时Star动画提升了用户体验

![](https://i.imgur.com/f61VqoI.gif)

拍摄功能
- 拍摄
- 录像
- 聚焦
- 翻转镜头

![](https://i.imgur.com/XjloOfT.gif)

发布功能
- 我们在普通的指定学号和用户名基础上增加了自定义输入的方式

![](https://i.imgur.com/M1Rufof.gif)

创新点——开屏动画
- 带来浸入式体验

![](https://i.imgur.com/Ejj8Loo.gif)

创新点——定位功能
- 获取地理位置并显示在首页

![](https://i.imgur.com/waESXdu.gif)

创新点——收藏功能
- 收藏内容通过列表形式呈现
- 长按可取消收藏

![](https://i.imgur.com/2gEsa0B.gif)![](https://i.imgur.com/JQOPjRU.gif)

难点分析
- 我们的定位功能仅需精确到城市，并不要求其他额外的功能。引入百度或者高德地图SDK过程复杂且有些得不偿失。
- 借助Android本身的LocationManager这个类提供的方法来获取经纬度，转换为城市信息
- 注：由于是谷歌提供的服务，在部分手机上效果并不好

![](https://i.imgur.com/fu4mfJg.png)
![](https://i.imgur.com/b2yrw7Z.png)

（in MainActivity）

总结

本项目综合运用了RecyclerView、Retrofit、数据库，Content Provider、多媒体等基本知识，完成了预期目标。

说明

详情可参阅Summary ppt\MiniDouyin项目总结.pptx


