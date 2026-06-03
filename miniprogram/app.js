App({
  onLaunch: function () {
    // API基础地址 - 开发环境使用本地地址，部署后修改为实际地址
    this.globalData.baseUrl = 'http://localhost:8049';
    // 部署时取消注释下面一行，注释上面一行
    // this.globalData.baseUrl = 'https://your-domain.com';

    // 从本地缓存恢复登录状态，避免每次冷启动都要重新授权
    try {
      const token = wx.getStorageSync('token');
      const userInfo = wx.getStorageSync('userInfo');

      if (token && userInfo) {
        this.globalData.userInfo = userInfo;
        this.globalData.isLoggedIn = true;
      } else {
        this.globalData.userInfo = null;
        this.globalData.isLoggedIn = false;
      }
    } catch (e) {
      this.globalData.userInfo = null;
      this.globalData.isLoggedIn = false;
    }
  },

  // 保存登录信息到本地缓存
  saveLoginInfo: function(token, userInfo) {
    this.globalData.userInfo = userInfo;
    this.globalData.isLoggedIn = true;
    try {
      wx.setStorageSync('token', token);
      wx.setStorageSync('userInfo', userInfo);
    } catch (e) {
      console.error('缓存登录信息失败:', e);
    }
  },

  // 清除登录信息
  clearLoginInfo: function() {
    this.globalData.userInfo = null;
    this.globalData.isLoggedIn = false;
    try {
      wx.removeStorageSync('token');
      wx.removeStorageSync('userInfo');
    } catch (e) {}
  },

  globalData: {
    baseUrl: '',
    userInfo: null,
    isLoggedIn: false
  }
});
