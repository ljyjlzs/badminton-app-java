// pages/rules/rules.js
const { get } = require('../../utils/request');

Page({
  data: {
    loading: true,
    currentTab: 'singles',
    sections: [],
    currentSection: null
  },

  onLoad: function () {
    this.loadRules();
  },

  onPullDownRefresh: function () {
    this.loadRules();
    wx.stopPullDownRefresh();
  },

  loadRules: function () {
    this.setData({ loading: true });
    get('/rules', {}, { showLoading: false })
      .then(res => {
        this.setData({ loading: false });
        const sections = res.data || [];
        const currentSection = sections.find(s => s.id === this.data.currentTab) || sections[0] || null;
        this.setData({
          sections: sections,
          currentSection: currentSection
        });
      })
      .catch(() => {
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      });
  },

  switchTab: function (e) {
    const tab = e.currentTarget.dataset.tab;
    const currentSection = this.data.sections.find(s => s.id === tab) || null;
    this.setData({
      currentTab: tab,
      currentSection: currentSection
    });
  }
});
