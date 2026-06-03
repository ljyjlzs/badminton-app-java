// 活动详情页面逻辑
const app = getApp();
const { get, post, put, del, getBaseUrl } = require('../../utils/request');

Page({
   data: {
     activityId: '',
     activity: null,
     registrations: [],
     matches: [],
     userRegistration: null,
     isOrganizer: false,
     pendingCancelCount: 0,
     loading: true,
     showLevelPicker: false,
     showCancelModal: false,
     cancelModalItem: null,
     selectedLevel: 5,
    inputNickname: '',
    inputAvatar: '',
    inputPartnerId: '',
    availablePartners: [],
     levelDesc: ['萌新·娱乐场', '新手·养生球', '新手·进阶', '初学者提高', '熟手·小对抗', '中级爱好者', '中高级玩家', '高级爱好者', '高手·大对抗', '专业水平'],

   stopPropagation: function() {},
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
    if (options.id) {
      this.setData({ activityId: options.id });
      this.loadActivityDetail();
    }
  },

  onShow: function() {
    if (this.data.activityId) {
      this.loadActivityDetail();
    }
  },

  loadActivityDetail: function() {
    this.setData({ loading: true });

    get(`/activity/${this.data.activityId}`, null, { showLoading: false })
      .then(res => {
        this.setData({ loading: false });

        const data = res.data || {};
        const activity = data.activity || {};

        if (!activity.type || (activity.type !== 'singles' && activity.type !== 'doubles' && activity.type !== 'fixed-doubles')) {
          activity.type = 'doubles';
        }

        const ts = activity.time;
        let formattedTime = '待定';
        if (ts && typeof ts === 'number') {
          const d = new Date(ts);
          formattedTime = `${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
        } else if (ts && typeof ts === 'string') {
          const d = new Date(ts);
          if (!isNaN(d.getTime())) {
            formattedTime = `${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
          }
        }
        activity.formattedTime = formattedTime;

        const matches = data.matches || [];
        const hasGroupMatches = matches.some(m => m.round === 'group');
        const hasChallengeMatches = matches.some(m => m.round === 'challenge');
        const hasFinalMatches = matches.some(m => m.round === 'final');
        const allGroupFinished = hasGroupMatches && matches.filter(m => m.round === 'group').every(m => m.status === 'confirmed');
        const allChallengeFinished = hasChallengeMatches && matches.filter(m => m.round === 'challenge').every(m => m.status === 'confirmed');

        this.setData({
          activity: activity,
          registrations: data.registrations || [],
          matches: matches,
          userRegistration: data.userRegistration || null,
          isOrganizer: data.isOrganizer || false,
          pendingCancelCount: data.pendingCancelCount || 0,
          hasChallengeMatches: hasChallengeMatches,
          hasFinalMatches: hasFinalMatches,
          allGroupFinished: allGroupFinished,
          allChallengeFinished: allChallengeFinished
        });

        // Java后端不支持实时监听，使用定时刷新替代
        this.startPolling();
      })
      .catch(err => {
        this.setData({ loading: false });
        console.error('加载活动详情失败：', err);
      });
  },

  // 定时轮询替代实时监听
  pollingTimer: null,

  startPolling: function() {
    this.stopPolling();
    // 每30秒刷新一次活动详情
    this.pollingTimer = setInterval(() => {
      if (this.data.activityId) {
        this.loadActivityDetail();
      }
    }, 30000);
  },

  stopPolling: function() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  },

  onUnload: function() {
    this.stopPolling();
  },

  onHide: function() {
    this.stopPolling();
  },

  showLevelPicker: function() {
    // 活动报名期间才能修改等级
    if (this.data.activity && this.data.activity.status !== 'registering') {
      wx.showToast({
        title: '活动已开始，无法修改等级',
        icon: 'none'
      });
      return;
    }
    const userInfo = app.globalData.userInfo || {};
    const activity = this.data.activity || {};
    const isFixedDoubles = activity.type === 'fixed-doubles';

    // 计算可选搭档列表
    const availablePartners = isFixedDoubles ? this.getAvailablePartners() : [];

    this.setData({
      showLevelPicker: true,
      selectedLevel: this.data.userRegistration?.level || 5,
      pickerLevel: (this.data.userRegistration?.level || 5) - 1,
      inputNickname: userInfo.nickname || '',
      inputAvatar: userInfo.avatar || '',
      inputPartnerId: '',
      availablePartners: availablePartners
    });
  },

  onNicknameInput: function(e) {
    this.setData({
      inputNickname: e.detail.value
    });
  },

  onChooseJoinAvatar: function(e) {
    const avatarUrl = e.detail.avatarUrl;
    if (avatarUrl) {
      this.uploadJoinAvatar(avatarUrl);
    }
  },

  onResetJoinAvatar: function() {
    this.setData({ inputAvatar: '' });
  },

  onSelectPartner: function(e) {
    const partnerId = e.currentTarget.dataset.partnerid;
    this.setData({ inputPartnerId: partnerId });
  },

  // 获取可选择的搭档列表（已报名但还没配对的玩家，排除自己）
  getAvailablePartners: function() {
    const registrations = this.data.registrations || [];
    const userInfo = app.globalData.userInfo || {};
    const userId = userInfo.id;
    return registrations.filter(r => {
      if (r.user_id === userId) return false;
      if (r.partner_id) return false;
      return true;
    });
  },

  uploadJoinAvatar: function(tempFilePath) {
    wx.showLoading({ title: '上传中...' });

    // 使用Java后端的文件上传接口
    wx.uploadFile({
      url: `${getBaseUrl()}/file/avatar`,
      filePath: tempFilePath,
      name: 'file',
      header: {
        'Authorization': `Bearer ${wx.getStorageSync('token')}`
      },
      success: (res) => {
        wx.hideLoading();
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 && data.data && data.data.url) {
            this.setData({ inputAvatar: data.data.url });
          } else {
            this.setData({ inputAvatar: tempFilePath });
            wx.showToast({ title: '头像可能仅自己可见', icon: 'none', duration: 2000 });
          }
        } catch (e) {
          this.setData({ inputAvatar: tempFilePath });
          wx.showToast({ title: '头像可能仅自己可见', icon: 'none', duration: 2000 });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('头像上传失败，使用临时路径:', err);
        this.setData({ inputAvatar: tempFilePath });
        wx.showToast({ title: '头像可能仅自己可见', icon: 'none', duration: 2000 });
      }
    });
  },

  uploadAvatarForJoin: function() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        this.uploadJoinAvatar(tempFilePath);
      }
    });
  },

  hideLevelPicker: function() {
    this.setData({ showLevelPicker: false });
  },

   onLevelChange: function(e) {
     const pickerIndex = parseInt(e.detail.value[0]);
     const newLevel = pickerIndex + 1;
     this.setData({
       pickerLevel: pickerIndex,
       selectedLevel: newLevel
     });
   },

   selectLevel: function(e) {
     const level = parseInt(e.currentTarget.dataset.level);
     this.setData({
       selectedLevel: level,
       pickerLevel: level - 1
     });
   },

  confirmLevel: function() {
    const pickerLevel = this.data.pickerLevel;
    const level = pickerLevel + 1;

    put('/user/level', { level })
      .then(res => {
        this.setData({
          'userRegistration.level': level,
          showLevelPicker: false
        });
        wx.showToast({
          title: '等级已更新',
          icon: 'success'
        });
        this.loadActivityDetail();
      })
      .catch(err => {
        console.error('修改等级失败:', err);
      });
  },

  joinActivity: function() {
    if (!this.data.userRegistration) {
      this.showLevelPicker();
    }
  },

  doJoin: function() {
    const nickname = this.data.inputNickname.trim();
    const avatar = this.data.inputAvatar;
    const level = this.data.selectedLevel;
    const activity = this.data.activity || {};
    const isFixedDoubles = activity.type === 'fixed-doubles';
    const partnerId = this.data.inputPartnerId || '';

    if (!nickname) {
      wx.showToast({
        title: '请输入您的名字',
        icon: 'none'
      });
      return;
    }

    if (nickname.length < 2) {
      wx.showToast({
        title: '名字至少2个字符',
        icon: 'none'
      });
      return;
    }

    if (isFixedDoubles && !partnerId && this.data.availablePartners.length > 0) {
      wx.showToast({ title: '双打固搭请选择搭档', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '报名中...' });

    post('/registration/join', {
      activityId: parseInt(this.data.activityId),
      activityName: activity.name || '',
      level: level,
      nickname: nickname,
      avatar: avatar,
      partnerId: isFixedDoubles && partnerId ? parseInt(partnerId) : null
    })
      .then(res => {
        wx.hideLoading();
        if (nickname && app.globalData.userInfo) {
          app.globalData.userInfo.nickname = nickname;
        }
        put('/user/info', { nickname }, { showLoading: false }).catch(() => {});
        wx.showToast({
          title: '报名成功',
          icon: 'success'
        });
        this.hideLevelPicker();
        this.loadActivityDetail();
      })
      .catch(err => {
        wx.hideLoading();
        console.error('报名失败:', err);
      });
  },

  startGrouping: function() {
    const activity = this.data.activity;
    const registrations = this.data.registrations;
    if (activity && activity.type !== 'singles' && registrations.length % 2 !== 0) {
      const typeLabel = activity.type === 'fixed-doubles' ? '双打固搭' : '双打';
      wx.showModal({
        title: '人数不足',
        content: `${typeLabel}比赛需要偶数人数，当前${registrations.length}人。请增加或减少1人后再开始分组。`,
        showCancel: false
      });
      return;
    }

    wx.showModal({
      title: '确认开始分组',
      content: '分组后不能再报名，是否继续？',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '分组中...' });

          post('/match/grouping', { activityId: parseInt(this.data.activityId) })
            .then(res => {
              wx.hideLoading();
              wx.showToast({
                title: '分组完成',
                icon: 'success'
              });
              this.loadActivityDetail();
            })
            .catch(err => {
              wx.hideLoading();
              console.error('分组失败:', err);
            });
        }
      }
    });
  },

  goToMatch: function(e) {
    const matchId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/match-score/match-score?activityId=${this.data.activityId}&matchId=${matchId}`
    });
  },

  goToGrouping: function() {
    wx.navigateTo({
      url: `/pages/grouping/grouping?activityId=${this.data.activityId}`
    });
  },

  startChallenge: function() {
    wx.showModal({
      title: '开始挑战赛',
      content: '确认所有小组赛已结束？挑战赛中胜者+10分',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '创建挑战赛...' });

          post('/challenge/start', { activityId: parseInt(this.data.activityId) })
            .then(res => {
              wx.hideLoading();
              wx.showToast({
                title: res.data?.message || '挑战赛已开始',
                icon: 'success'
              });
              this.loadActivityDetail();
            })
            .catch(err => {
              wx.hideLoading();
              console.error('创建挑战赛失败:', err);
            });
        }
      }
    });
  },

  startFinal: function() {
    wx.showModal({
      title: '开始决赛',
      content: '确认所有挑战赛已结束？决赛胜者+15分',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '创建决赛...' });

          post('/challenge/final', { activityId: parseInt(this.data.activityId) })
            .then(res => {
              wx.hideLoading();
              wx.showToast({
                title: '决赛已开始',
                icon: 'success'
              });
              this.loadActivityDetail();
            })
            .catch(err => {
              wx.hideLoading();
              console.error('创建决赛失败:', err);
            });
        }
      }
    });
  },

  startAllChallenge: function() {
    const matches = this.data.matches.filter(m => m.round === 'challenge' && m.status === 'pending');
    if (matches.length === 0) {
      wx.showToast({ title: '没有待开始的挑战赛', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '开始挑战赛',
      content: `确定开始 ${matches.length} 场挑战赛吗？`,
      success: res => {
        if (res.confirm) {
          this.doStartAllMatches(matches);
        }
      }
    });
  },

  startAllFinal: function() {
    const matches = this.data.matches.filter(m => m.round === 'final' && m.status === 'pending');
    if (matches.length === 0) {
      wx.showToast({ title: '没有待开始的决赛', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '开始决赛',
      content: `确定开始 ${matches.length} 场决赛吗？`,
      success: res => {
        if (res.confirm) {
          this.doStartAllMatches(matches);
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
      }
    }

    wx.hideLoading();

    if (failCount === 0) {
      wx.showToast({ title: '比赛已开始', icon: 'success' });
      this.loadActivityDetail();
    } else {
      wx.showToast({ title: `${successCount}场成功，${failCount}场失败`, icon: 'none' });
    }
  },

  goToRanking: function() {
    wx.navigateTo({
      url: `/pages/ranking/ranking?activityId=${this.data.activityId}`
    });
  },

  formatTime: function(timeValue) {
    if (!timeValue && timeValue !== 0) return '待定';

    let timestamp = timeValue;

    if (typeof timeValue === 'number') {
      timestamp = timeValue;
    } else if (typeof timeValue === 'string') {
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
    return `${month}月${day}日 ${hour}:${String(minute).padStart(2, '0')}`;
  },

  shareActivity: function() {
    wx.showShareMenu({
      withShareTicket: true
    });
  },

  deleteActivity: function() {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除此活动吗？此操作不可恢复。',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' });

          del(`/activity/${this.data.activityId}`)
            .then(res => {
              wx.hideLoading();
              wx.showToast({
                title: '删除成功',
                icon: 'success'
              });
              setTimeout(() => {
                wx.navigateBack();
              }, 1500);
            })
            .catch(err => {
              wx.hideLoading();
              console.error('删除活动失败:', err);
            });
        }
      }
    });
  },

  finishActivity: function() {
    wx.showModal({
      title: '结束活动',
      content: '确定要结束此活动吗？结束后可查看排名',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '结束中...' });

          put(`/activity/${this.data.activityId}/status`, { status: 'finished' })
            .then(res => {
              wx.hideLoading();
              wx.showToast({
                title: '活动已结束',
                icon: 'success'
              });
              this.loadActivityDetail();
            })
            .catch(err => {
              wx.hideLoading();
              console.error('结束活动失败:', err);
            });
        }
      }
    });
  },

  cancelRegistration: function() {
    const activity = this.data.activity;

    wx.showModal({
      title: '确认取消报名',
      content: '取消报名需要组织者确认后才会生效，确定要申请取消吗？',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '提交中...' });

          post('/registration/cancel', {
            activityId: parseInt(this.data.activityId),
            activityName: activity?.name || ''
          })
            .then(res => {
              wx.hideLoading();
              wx.showToast({ title: '已提交取消申请', icon: 'success' });
              this.loadActivityDetail();
            })
            .catch(err => {
              wx.hideLoading();
              console.error('取消报名失败:', err);
            });
        }
      }
    });
  },

  handleCancel: function(e) {
    const registrationId = e.currentTarget.dataset.id;
    const action = e.currentTarget.dataset.action;
    const actionText = action === 'approve' ? '同意' : '拒绝';

    wx.showModal({
      title: `${actionText}取消报名`,
      content: `确定${actionText}该用户的取消报名请求吗？`,
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' });

          post('/registration/handle-cancel', {
            registrationId: parseInt(registrationId),
            action: action
          })
            .then(res => {
              wx.hideLoading();
              wx.showToast({ title: res.message || '处理成功', icon: 'success' });
              this.loadActivityDetail();
            })
            .catch(err => {
              wx.hideLoading();
              console.error('处理取消请求失败:', err);
            });
        }
      }
    });
  },

  onRegItemTap: function(e) {
    const item = e.currentTarget.dataset.item;
    if (!item || item.cancel_status !== 'pending' || !this.data.isOrganizer) return;

    this.setData({
      showCancelModal: true,
      cancelModalItem: item
    });
  },

  hideCancelModal: function() {
    this.setData({ showCancelModal: false, cancelModalItem: null });
  },

  handleCancelFromModal: function(e) {
    const action = e.currentTarget.dataset.action;
    const item = this.data.cancelModalItem;
    if (!item) return;

    this.setData({ showCancelModal: false, cancelModalItem: null });

    wx.showLoading({ title: '处理中...' });

    post('/registration/handle-cancel', {
      registrationId: parseInt(item.id || item._id),
      action: action
    })
      .then(res => {
        wx.hideLoading();
        wx.showToast({ title: res.message || '处理成功', icon: 'success' });
        this.loadActivityDetail();
      })
      .catch(err => {
        wx.hideLoading();
        console.error('处理取消请求失败:', err);
      });
  },

  copyLocation: function(e) {
    const activity = this.data.activity;
    if (activity && activity.latitude && activity.longitude) {
      wx.openLocation({
        latitude: activity.latitude,
        longitude: activity.longitude,
        name: activity.name,
        address: activity.location,
        scale: 18
      });
    } else {
      const location = e.currentTarget.dataset.location;
      if (location) {
        wx.setClipboardData({
          data: location,
          success: () => {
            wx.showToast({
              title: '地点已复制',
              icon: 'success'
            });
          }
        });
      }
    }
  }
});
