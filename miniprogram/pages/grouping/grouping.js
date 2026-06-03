const app = getApp();
const { get, post, put } = require('../../utils/request');

Page({
  data: {
    activityId: '',
    activity: null,
    teams: [],
    matches: [],
    loading: true,
    isOrganizer: false,
    namingTeamId: null,
    teamName: '',
    completedCount: 0
  },

  stopPropagation: function() {},

  onLoad: function(options) {
    if (options.activityId) {
      this.setData({ activityId: options.activityId });
      this.loadGroupingData();
    }
  },

  onShow: function() {
    if (this.data.activityId) {
      this.loadGroupingData();
    }
  },

  loadGroupingData: function() {
    this.setData({ loading: true });

    get(`/activity/${this.data.activityId}`, null, { showLoading: false })
      .then(res => {
        this.setData({ loading: false });

        const data = res.data || {};
        const matches = data.matches || [];
        const completedCount = matches.filter(m => m.status === 'confirmed').length;

        this.setData({
          activity: data.activity || null,
          teams: data.teams || [],
          matches: matches,
          completedCount: completedCount,
          isOrganizer: data.isOrganizer || false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        console.error('加载分组数据失败：', err);
      });
  },

  startNaming: function(e) {
    const teamId = e.currentTarget.dataset.teamid;
    this.setData({
      namingTeamId: teamId,
      teamName: ''
    });
  },

  onTeamNameInput: function(e) {
    this.setData({
      teamName: e.detail.value
    });
  },

  confirmTeamName: function() {
    if (!this.data.teamName.trim()) {
      wx.showToast({
        title: '请输入队名',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '保存中...' });

    put(`/team/${this.data.namingTeamId}/name`, {
      name: this.data.teamName.trim()
    })
      .then(res => {
        wx.hideLoading();
        wx.showToast({
          title: '命名成功',
          icon: 'success'
        });
        this.setData({ namingTeamId: null });
        this.loadGroupingData();
      })
      .catch(err => {
        wx.hideLoading();
        console.error('命名失败:', err);
      });
  },

  cancelNaming: function() {
    this.setData({
      namingTeamId: null,
      teamName: ''
    });
  },

  goToMatchScore: function(e) {
    const matchId = e.currentTarget.dataset.matchid;
    wx.navigateTo({
      url: `/pages/match-score/match-score?activityId=${this.data.activityId}&matchId=${matchId}`
    });
  },

  startAllMatches: function() {
    const matches = this.data.matches;
    const pendingMatches = matches.filter(m => m.status === 'pending');

    if (pendingMatches.length === 0) {
      wx.showToast({ title: '没有待开始的比赛', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '开始比赛',
      content: `确定开始 ${pendingMatches.length} 场比赛吗？`,
      success: res => {
        if (res.confirm) {
          this.doStartAllMatches(pendingMatches);
        }
      }
    });
  },

  doStartAllMatches: async function(matches) {
    wx.showLoading({ title: '开始中...' });

    let successCount = 0;
    let failCount = 0;

    for (const match of matches) {
      try {
        await post('/match/start', {
          activityId: parseInt(this.data.activityId),
          matchId: parseInt(match.id || match._id)
        }, { showLoading: false });
        successCount++;
      } catch (e) {
        failCount++;
        console.error('开始比赛失败:', e);
      }
    }

    wx.hideLoading();

    if (failCount === 0) {
      put(`/activity/${this.data.activityId}/status`, { status: 'playing' }, { showLoading: false })
        .then(() => {
          wx.showToast({ title: '比赛已开始', icon: 'success' });
          this.loadGroupingData();
        })
        .catch(() => {
          wx.showToast({ title: '比赛已开始', icon: 'success' });
          this.loadGroupingData();
        });
    } else {
      wx.showToast({ title: `${successCount}场成功，${failCount}场失败`, icon: 'none' });
    }
  },

  goToRanking: function() {
    wx.navigateTo({
      url: `/pages/ranking/ranking?activityId=${this.data.activityId}`
    });
  }
});
