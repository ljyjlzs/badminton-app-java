App({
  onLaunch: function () {
    // API基础地址
    this.globalData.baseUrl = 'http://127.0.0.1:8049';

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
