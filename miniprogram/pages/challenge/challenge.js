// 挑战赛页面
const app = getApp();
const { get, post } = require('../../utils/request');

Page({
  data: {
    activityId: '',
    qualifiedTeams: [],
    eliminatedPlayers: [],
    challengeMatch: null,
    loading: true,
    isOrganizer: false,
    selectedPartner: null
  },

  onLoad: function(options) {
    if (options.activityId) {
      this.setData({ activityId: options.activityId });
      this.loadChallengeData();
    }
  },

  onShow: function() {
    if (this.data.activityId) {
      this.loadChallengeData();
    }
  },

  loadChallengeData: function() {
    this.setData({ loading: true });

    get(`/challenge/${this.data.activityId}`, null, { showLoading: false })
      .then(res => {
        this.setData({ loading: false });

        const data = res.data || {};
        this.setData({
          qualifiedTeams: data.qualifiedTeams || [],
          eliminatedPlayers: data.eliminatedPlayers || [],
          challengeMatch: data.challengeMatch || null,
          isOrganizer: data.isOrganizer || false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        console.error('加载挑战赛数据失败：', err);
      });
  },

  formChallengeTeam: function(e) {
    const playerId = e.currentTarget.dataset.playerid;
    const selectedPlayer = this.data.eliminatedPlayers.find(p => p.userId === playerId);

    if (!this.data.selectedPartner) {
      this.setData({ selectedPartner: selectedPlayer });
      wx.showToast({
        title: '请选择搭档',
        icon: 'none'
      });
    } else {
      this.createChallengeTeam(this.data.selectedPartner.userId, playerId);
    }
  },

  createChallengeTeam: function(player1Id, player2Id) {
    // 调用后端创建挑战队接口
    post('/challenge/start', {
      activityId: parseInt(this.data.activityId),
      player1Id: player1Id,
      player2Id: player2Id
    })
      .then(res => {
        wx.showToast({
          title: '挑战队已创建',
          icon: 'success'
        });
        this.setData({ selectedPartner: null });
        this.loadChallengeData();
      })
      .catch(err => {
        console.error('创建挑战队失败:', err);
      });
  },

  startChallenge: function() {
    if (!this.data.challengeMatch) {
      wx.showToast({
        title: '请先组建挑战队',
        icon: 'none'
      });
      return;
    }

    wx.navigateTo({
      url: `/pages/match-score/match-score?activityId=${this.data.activityId}&matchId=${this.data.challengeMatch.id || this.data.challengeMatch._id}`
    });
  }
});
