// 活动列表页面
const app = getApp();
const { get } = require('../../utils/request');

// 活动类型中文映射
const TYPE_MAP = {
  'singles': '单打',
  'doubles': '双打轮换',
  'fixed-doubles': '双打固搭'
};

Page({
  data: {
    myCreatedActivities: [],
    myJoinedActivities: [],
    availableActivities: [],
    filteredCreatedActivities: [],
    filteredJoinedActivities: [],
    searchResults: [],
    searchKeyword: '',
    currentTab: 'created',
    loading: true,
    searchLoading: false,
    statusText: {
      'registering': '报名中',
      'grouping': '分组中',
      'playing': '比赛中',
      'challenge': '挑战赛',
      'final': '决赛',
      'finished': '已结束'
    }
  },

  onLoad: function(options) {
    const targetTab = app.globalData.activityListTab || 'created';
    this.setData({ currentTab: targetTab });
    if (app.globalData.isLoggedIn) {
      this.loadActivities();
    }
  },

  onShow: function() {
    if (!app.globalData.isLoggedIn) {
      wx.showModal({
        title: '需要授权',
        content: '请先在首页授权登录后查看活动',
        showCancel: false,
        success: () => {
          wx.switchTab({ url: '/pages/index/index' });
        }
      });
      return;
    }
    this.loadActivities();
  },

  onPullDownRefresh: function() {
    this.loadActivities();
    wx.stopPullDownRefresh();
  },

  loadActivities: function() {
    this.setData({ loading: true });

    // 加载我创建的活动
    get('/activity/list', { type: 'organized' }, { showLoading: false })
      .then(res => {
        this.setData({ myCreatedActivities: res.data || [] });
        this.applyFilter();
      })
      .catch(() => {});

    // 加载我参加的活动
    get('/activity/list', { type: 'joined' }, { showLoading: false })
      .then(res => {
        this.setData({
          loading: false,
          myJoinedActivities: res.data || []
        });
        this.applyFilter();
      })
      .catch(() => {
        this.setData({ loading: false });
      });

    // 加载可参加的活动
    get('/activity/list', { type: 'available' }, { showLoading: false })
      .then(res => {
        this.setData({ availableActivities: res.data || [] });
        if (this.data.currentTab === 'search' && !this.data.searchKeyword) {
          this.setData({ searchResults: res.data || [] });
        }
      })
      .catch(() => {});
  },

  switchTab: function(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });

    if (tab === 'search') {
      if (this.data.searchKeyword) {
        this.doCloudSearch();
      } else {
        this.setData({ searchResults: this.data.availableActivities });
      }
    } else {
      this.applyFilter();
    }
  },

  onSearchInput: function(e) {
    this.setData({ searchKeyword: e.detail.value });
    if (this._searchTimer) clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => {
      if (this.data.currentTab === 'search') {
        this.doCloudSearch();
      } else {
        this.applyFilter();
      }
    }, 300);
  },

  doSearch: function() {
    if (this.data.currentTab === 'search') {
      this.doCloudSearch();
    } else {
      this.applyFilter();
    }
  },

  applyFilter: function() {
    const kw = (this.data.searchKeyword || '').trim().toLowerCase();

    if (!kw) {
      this.setData({
        filteredCreatedActivities: this.data.myCreatedActivities,
        filteredJoinedActivities: this.data.myJoinedActivities
      });
      return;
    }

    const filterFn = (item) => {
      const nameMatch = item.name && item.name.toLowerCase().includes(kw);
      const locationMatch = item.location && item.location.toLowerCase().includes(kw);
      const typeCn = TYPE_MAP[item.type] || '';
      const typeMatch = typeCn.includes(kw) || (item.type && item.type.toLowerCase().includes(kw));
      return nameMatch || locationMatch || typeMatch;
    };

    this.setData({
      filteredCreatedActivities: this.data.myCreatedActivities.filter(filterFn),
      filteredJoinedActivities: this.data.myJoinedActivities.filter(filterFn)
    });
  },

  doCloudSearch: function() {
    const keyword = this.data.searchKeyword.trim();
    if (!keyword) {
      this.setData({ searchResults: [] });
      return;
    }

    this.setData({ searchLoading: true });

    get('/activity/list', { type: 'search', keyword }, { showLoading: false })
      .then(res => {
        this.setData({
          searchLoading: false,
          searchResults: res.data || []
        });
      })
      .catch(() => {
        this.setData({ searchLoading: false, searchResults: [] });
      });
  },

  clearSearch: function() {
    this.setData({
      searchKeyword: '',
      searchResults: this.data.availableActivities,
      filteredCreatedActivities: this.data.myCreatedActivities,
      filteredJoinedActivities: this.data.myJoinedActivities
    });
  },

  goToActivity: function(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activity-detail/activity-detail?id=${id}`
    });
  },

  goToCreate: function() {
    wx.navigateTo({
      url: '/pages/create-activity/create-activity'
    });
  },

  formatTime: function(timeValue) {
    if (!timeValue) return '待定';

    let timestamp = timeValue;

    if (typeof timeValue === 'string') {
      if (timeValue.includes('T')) {
        timestamp = new Date(timeValue).getTime();
      } else if (!isNaN(timeValue)) {
        timestamp = parseInt(timeValue);
      } else {
        return '待定';
      }
    } else if (typeof timeValue === 'object') {
      if (timeValue.$date) {
        timestamp = timeValue.$date;
      } else {
        return '待定';
      }
    }

    if (!timestamp || isNaN(timestamp)) return '待定';

    const date = new Date(timestamp);
    if (isNaN(date.getTime())) return '待定';

    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hour = date.getHours();
    const minute = date.getMinutes();
    return `${month}月${day}日 ${hour}:${minute < 10 ? '0' + minute : minute}`;
  }
});
