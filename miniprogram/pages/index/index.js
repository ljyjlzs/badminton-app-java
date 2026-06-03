// 首页逻辑
const app = getApp();
const { get, post, put, getBaseUrl } = require('../../utils/request');

Page({
  data: {
    userInfo: null,
    isLoggedIn: false,
    myActivities: [],
    joinableActivities: [],
    unavailableActivities: [],
    loading: true,
    showEditDialog: false,
    editType: 'nickname',
    editNickname: ''
  },

  onLoad: function() {
    this.checkLogin();
  },

  onShow: function() {
    if (this.data.isLoggedIn) {
      this.loadActivities();
    }
  },

  checkLogin: function() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');

    if (token && userInfo) {
      app.globalData.userInfo = userInfo;
      app.globalData.isLoggedIn = true;
      this.setData({
        userInfo: userInfo,
        isLoggedIn: true
      });
      this.loadActivities();
    } else {
      this.setData({
        loading: false
      });
    }
  },

  onGetUserInfo: function(e) {
    if (e.detail.userInfo) {
      const userInfo = e.detail.userInfo;
      this.setData({
        userInfo: userInfo,
        loading: true
      });

      // 调用微信登录
      wx.login({
        success: (loginRes) => {
          post('/user/login', { code: loginRes.code }, { showLoading: false })
            .then(res => {
              this.setData({ loading: false });
              const { token, userInfo: serverUserInfo } = res.data;

              // 保存登录信息
              wx.setStorageSync('token', token);
              const fullUserInfo = { ...userInfo, ...serverUserInfo };
              wx.setStorageSync('userInfo', fullUserInfo);

              app.globalData.userInfo = fullUserInfo;
              app.globalData.isLoggedIn = true;

              this.setData({
                isLoggedIn: true,
                userInfo: fullUserInfo
              });

              this.loadActivities();
            })
            .catch(err => {
              this.setData({ loading: false });
              wx.showToast({ title: '登录失败', icon: 'none' });
            });
        },
        fail: () => {
          this.setData({ loading: false });
          wx.showToast({ title: '微信登录失败', icon: 'none' });
        }
      });
    }
  },

  loadActivities: function() {
    // 加载我创建的活动
    get('/activity/list', { type: 'organized' }, { showLoading: false })
      .then(res => {
        this.setData({ myActivities: res.data || [] });
      })
      .catch(() => {});

    // 加载可参加的活动
    get('/activity/list', { type: 'available' }, { showLoading: false })
      .then(res => {
        this.setData({ joinableActivities: res.data || [] });
      })
      .catch(() => {});

    // 加载进行中的活动
    get('/activity/list', { type: 'unavailable' }, { showLoading: false })
      .then(res => {
        this.setData({ unavailableActivities: res.data || [] });
      })
      .catch(() => {});
  },

  goToCreateActivity: function() {
    wx.navigateTo({
      url: '/pages/create-activity/create-activity'
    });
  },

  goToActivityDetail: function(e) {
    const activityId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activity-detail/activity-detail?id=${activityId}`
    });
  },

  goToActivityList: function(e) {
    const tab = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.tab : 'search';
    const app = getApp();
    app.globalData.activityListTab = tab || 'search';
    wx.switchTab({
      url: '/pages/activity-list/activity-list'
    });
  },

  goToRules: function() {
    wx.navigateTo({
      url: '/pages/rules/rules'
    });
  },

  onEditNickname: function() {
    const userInfo = this.data.userInfo;
    this.setData({
      showEditDialog: true,
      editType: 'nickname',
      editNickname: userInfo && userInfo.nickname ? userInfo.nickname : ''
    });
  },

  onEditNicknameInput: function(e) {
    this.setData({ editNickname: e.detail.value });
  },

  hideEditDialog: function() {
    this.setData({ showEditDialog: false });
  },

  stopPropagation: function() {},

  confirmEdit: function() {
    const nickname = this.data.editNickname.trim();
    if (!nickname || nickname.length < 2) {
      wx.showToast({ title: '昵称至少2个字符', icon: 'none' });
      return;
    }

    put('/user/info', { nickname })
      .then(() => {
        const userInfo = app.globalData.userInfo || {};
        userInfo.nickname = nickname;
        app.globalData.userInfo = userInfo;
        wx.setStorageSync('userInfo', userInfo);

        this.setData({
          userInfo: userInfo,
          showEditDialog: false
        });
        wx.showToast({ title: '昵称已更新', icon: 'success' });
      })
      .catch(() => {
        wx.showToast({ title: '更新失败', icon: 'none' });
      });
  },

  onChooseAvatar: function(e) {
    const avatarUrl = e.detail.avatarUrl;
    if (avatarUrl) {
      // 上传头像到服务器
      wx.uploadFile({
        url: `${getBaseUrl()}/file/avatar`,
        filePath: avatarUrl,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + wx.getStorageSync('token')
        },
        success: (res) => {
          const data = JSON.parse(res.data);
          if (data.code === 200) {
            const avatar = data.data.url;
            put('/user/info', { avatar })
              .then(() => {
                const userInfo = app.globalData.userInfo || {};
                userInfo.avatar = avatar;
                app.globalData.userInfo = userInfo;
                wx.setStorageSync('userInfo', userInfo);
                this.setData({ userInfo: userInfo });
                wx.showToast({ title: '头像已更新', icon: 'success' });
              });
          }
        }
      });
    }
  }
});
