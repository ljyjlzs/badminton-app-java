// pages/ai-chat/ai-chat.js
const app = getApp();
const { get, post, del } = require('../../utils/request');

Page({
  data: {
    messages: [],
    inputValue: '',
    loading: false,
    loadingHistory: false,
    scrollToView: ''
  },

  onShow: function() {
    if (!app.globalData.isLoggedIn) {
      wx.showModal({
        title: '需要授权',
        content: '请先在首页授权登录后使用AI功能',
        showCancel: false,
        success: () => {
          wx.switchTab({ url: '/pages/index/index' });
        }
      });
      return;
    }

    const nickname = (app.globalData.userInfo && app.globalData.userInfo.nickname) || '';
    if (!nickname) {
      wx.showModal({
        title: '需要设置昵称',
        content: '请先在首页设置昵称后使用AI功能',
        showCancel: false,
        success: () => {
          wx.switchTab({ url: '/pages/index/index' });
        }
      });
      return;
    }
    this.loadHistory();
  },

  loadHistory: function() {
    this.setData({ loadingHistory: true });
    get('/ai/history', {}, { showLoading: false })
      .then(res => {
        this.setData({ loadingHistory: false });
        const msgs = (res.data || [])
          .filter(m => m.role !== 'system_feedback')
          .map(m => this._parseMessage(m));
        this.setData({ messages: msgs });
        this.scrollToBottom('');
      })
      .catch(() => {
        this.setData({ loadingHistory: false });
      });
  },

  _parseMessage: function(m) {
    const base = {
      ...m,
      timeStr: this.formatTime(m.created_at),
      hasAction: false,
      actionData: null,
      hasSelect: false,
      selectOptions: [],
      displayContent: m.content,
      actionStatus: ''
    };

    if (m.role !== 'assistant') return base;

    // 匹配 __SELECT__[...]__END__
    const selectMatch = m.content.match(/__SELECT__(\[[\s\S]*?\])__END__/);
    if (selectMatch) {
      try {
        const selectOptions = JSON.parse(selectMatch[1]);
        const displayContent = m.content.replace(/__SELECT__[\s\S]*?__END__/, '').trim();
        return {
          ...base,
          hasSelect: true,
          selectOptions: selectOptions,
          displayContent: displayContent
        };
      } catch (e) {}
    }

    // 匹配 __ACTION__{...}__END__
    const actionMatch = m.content.match(/__ACTION__(\{[\s\S]*?\})__END__/);
    if (!actionMatch) return base;

    try {
      const actionData = JSON.parse(actionMatch[1]);
      const displayContent = m.content.replace(/__ACTION__[\s\S]*?__END__/, '').trim();
      return {
        ...base,
        hasAction: true,
        actionData: actionData,
        displayContent: displayContent,
        actionStatus: 'pending'
      };
    } catch (e) {
      return base;
    }
  },

  onInput: function(e) {
    this.setData({ inputValue: e.detail.value });
  },

  sendMessage: function() {
    const content = (this.data.inputValue || '').trim();
    if (!content || this.data.loading) return;

    const userMsg = {
      _id: 'tmp_' + Date.now(),
      role: 'user',
      content: content,
      displayContent: content,
      hasAction: false,
      hasSelect: false,
      actionStatus: '',
      timeStr: '刚刚'
    };

    this.setData({
      inputValue: '',
      loading: true,
      messages: [...this.data.messages, userMsg]
    });
    this.scrollToBottom(String(Date.now()));

    this._callAI({ message: content });
  },

  _callAI: function(params) {
    const timeoutId = setTimeout(() => {
      if (this.data.loading) {
        this.setData({ loading: false });
        wx.showToast({ title: 'AI响应超时，请重试', icon: 'none', duration: 3000 });
      }
    }, 30000);

    post('/ai/chat', params, { showLoading: false })
      .then(res => {
        clearTimeout(timeoutId);
        this.setData({ loading: false });
        const raw = {
          _id: 'ai_' + Date.now(),
          role: 'assistant',
          content: res.data.content,
          created_at: null
        };
        const aiMsg = this._parseMessage(raw);
        aiMsg.timeStr = '刚刚';
        this.setData({
          messages: [...this.data.messages, aiMsg]
        });
        this.scrollToBottom(String(Date.now()));
      })
      .catch(err => {
        clearTimeout(timeoutId);
        this.setData({ loading: false });
        const errMsg = {
          _id: 'err_' + Date.now(),
          role: 'assistant',
          content: '抱歉，AI暂时无法响应：' + (err.message || '未知错误'),
          displayContent: '抱歉，AI暂时无法响应：' + (err.message || '未知错误'),
          hasAction: false,
          hasSelect: false,
          actionStatus: '',
          timeStr: '刚刚'
        };
        this.setData({
          messages: [...this.data.messages, errMsg]
        });
      });
  },

  onConfirmAction: function(e) {
    const idx = e.currentTarget.dataset.index;
    const messages = this.data.messages;
    const msg = messages[idx];
    if (!msg || !msg.hasAction || msg.actionStatus !== 'pending') return;

    const action = msg.actionData;
    const actionType = action.action;

    const newMessages = [...messages];
    newMessages[idx] = { ...msg, actionStatus: 'executing' };
    this.setData({ messages: newMessages });

    if (actionType === 'join_activity') {
      this._doJoinActivity(idx, action.params);
    } else if (actionType === 'cancel_registration') {
      this._doCancelRegistration(idx, action.params);
    } else {
      this._finishAction(idx, false, '未知操作类型：' + actionType);
    }
  },

  onCancelAction: function(e) {
    const idx = e.currentTarget.dataset.index;
    const messages = this.data.messages;
    const msg = messages[idx];
    if (!msg || !msg.hasAction) return;

    const newMessages = [...messages];
    newMessages[idx] = { ...msg, actionStatus: 'cancelled' };
    this.setData({ messages: newMessages, loading: true });

    const cancelText = '[系统通知] 用户取消了操作：' + (msg.actionData.confirm_text || msg.actionData.action);
    this._callAI({ action_result: cancelText });
  },

  onSelectActivity: function(e) {
    const activity = e.currentTarget.dataset.activity;
    if (!activity) return;

    const selectText = '我选择报名「' + activity.name + '」';
    this.setData({ loading: true });

    const userMsg = {
      _id: 'tmp_' + Date.now(),
      role: 'user',
      content: selectText,
      displayContent: selectText,
      hasAction: false,
      hasSelect: false,
      actionStatus: '',
      timeStr: '刚刚'
    };
    this.setData({
      messages: [...this.data.messages, userMsg]
    });
    this.scrollToBottom(String(Date.now()));

    this._callAI({ message: selectText });
  },

  _doJoinActivity: function(idx, params) {
    const userInfo = app.globalData && app.globalData.userInfo;
    if (!userInfo || !userInfo.nickname) {
      this._finishAction(idx, false, '参加活动失败：请先在「首页」完善个人信息');
      return;
    }

    post('/registration/join', {
      activityId: params.activityId || '',
      activityName: params.activityName || '',
      nickname: userInfo.nickname,
      level: userInfo.level || 5,
      avatar: userInfo.avatar || ''
    }, { showLoading: false })
      .then(res => {
        this._finishAction(idx, true, '已成功报名参加活动「' + params.activityName + '」！');
      })
      .catch(err => {
        this._finishAction(idx, false, '报名失败：' + (err.message || '未知错误'));
      });
  },

  _doCancelRegistration: function(idx, params) {
    post('/registration/cancel', {
      activityId: params.activityId || '',
      activityName: params.activityName || '',
      reason: '用户主动取消'
    }, { showLoading: false })
      .then(res => {
        this._finishAction(idx, true, '已提交取消报名申请（活动：「' + params.activityName + '」），等待组织者审批。');
      })
      .catch(err => {
        this._finishAction(idx, false, '取消报名失败：' + (err.message || '未知错误'));
      });
  },

  _finishAction: function(idx, success, resultText) {
    const messages = this.data.messages;
    const newMessages = [...messages];
    newMessages[idx] = {
      ...messages[idx],
      actionStatus: success ? 'success' : 'error',
      actionResultText: resultText
    };
    this.setData({ messages: newMessages, loading: true });

    this._callAI({ action_result: resultText });
  },

  _handleMultipleMatches: function(idx, matches) {
    const messages = this.data.messages;
    const newMessages = [...messages];

    const selectMsg = {
      _id: 'select_' + Date.now(),
      role: 'assistant',
      content: '',
      displayContent: '我找到了以下可报名的活动，请选择：',
      hasAction: false,
      hasSelect: true,
      selectOptions: matches.map(m => ({
        id: m.id,
        name: m.name,
        time: this.formatTime(m.time),
        location: m.location || '待定'
      })),
      actionStatus: '',
      timeStr: '刚刚'
    };

    newMessages[idx] = selectMsg;
    this.setData({ messages: newMessages, loading: false });
    this.scrollToBottom(String(Date.now()));
  },

  scrollToBottom: function(suffix) {
    this.setData({ scrollToView: '' });
    setTimeout(() => {
      this.setData({ scrollToView: 'msg-bottom-' + suffix });
    }, 100);
  },

  formatTime: function(timeValue) {
    if (!timeValue) return '';
    let ts = timeValue;
    if (typeof timeValue === 'object' && timeValue.$date) {
      ts = timeValue.$date;
    }
    const d = new Date(ts);
    const h = d.getHours();
    const m = String(d.getMinutes()).padStart(2, '0');
    return h + ':' + m;
  },

  clearHistory: function() {
    wx.showModal({
      title: '新对话',
      content: '确定要清空所有聊天记录，开始新对话吗？',
      success: res => {
        if (res.confirm) {
          del('/ai/history', {}, { showLoading: false })
            .then(() => {
              this.setData({ messages: [] });
              wx.showToast({ title: '已开始新对话', icon: 'success' });
            });
        }
      }
    });
  }
});
