// 排名页面逻辑
const app = getApp();
const { get } = require('../../utils/request');

Page({
  data: {
    activityId: '',
    activity: null,
    individualRankings: [],
    teamRankings: [],
    currentTab: 'individual',
    loading: true
  },

  tabs: ['individual', 'team'],

  pollingTimer: null,

  onLoad: function(options) {
    if (options.activityId) {
      this.setData({ activityId: options.activityId });
      this.loadRankings();
    }
  },

  onShow: function() {
    if (this.data.activityId && !this.data.loading) {
      this.loadRankings();
    }
    this.startPolling();
  },

  onHide: function() {
    this.stopPolling();
  },

  onUnload: function() {
    this.stopPolling();
  },

  startPolling: function() {
    this.stopPolling();
    // 每30秒刷新一次排名
    this.pollingTimer = setInterval(() => {
      if (this.data.activityId) {
        this.loadRankings();
      }
    }, 30000);
  },

  stopPolling: function() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  },

  loadRankings: function() {
    this.setData({ loading: true });

    get(`/ranking/${this.data.activityId}`, { type: 'all' }, { showLoading: false })
      .then(res => {
        this.setData({ loading: false });

        const data = res.data || {};
        this.setData({
          activity: data.activity || null,
          individualRankings: data.individualRankings || [],
          teamRankings: data.teamRankings || []
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        console.error('加载排名失败：', err);
      });
  },

  switchTab: function(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
  },

  getRankClass: function(rank) {
    if (rank === 1) return 'gold';
    if (rank === 2) return 'silver';
    if (rank === 3) return 'bronze';
    return '';
  },

  getScoreClass: function(score) {
    if (score > 0) return 'positive';
    if (score < 0) return 'negative';
    return '';
  }
});
