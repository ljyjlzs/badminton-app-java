/**
 * HTTP请求封装
 * 替代 wx.cloud.callFunction
 */

// 获取API基础地址
const getBaseUrl = () => {
  const app = getApp();
  return (app && app.globalData && app.globalData.baseUrl ? app.globalData.baseUrl : '') + '/api';
};

/**
 * 统一请求方法
 * @param {Object} options 请求选项
 * @param {string} options.url 请求路径
 * @param {string} options.method 请求方法
 * @param {Object} options.data 请求数据
 * @param {boolean} options.showLoading 是否显示加载提示
 */
const request = (options = {}) => {
  return new Promise((resolve, reject) => {
    // 获取token
    const token = wx.getStorageSync('token');

    // 显示加载提示
    if (options.showLoading !== false) {
      wx.showLoading({ title: '加载中...' });
    }

    wx.request({
      url: getBaseUrl() + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Authorization': token ? `Bearer ${token}` : '',
        'Content-Type': 'application/json'
      },
      success: (res) => {
        wx.hideLoading();

        if (res.statusCode === 200) {
          const data = res.data;
          if (data.code === 200) {
            resolve(data);
          } else {
            wx.showToast({ title: data.message || '请求失败', icon: 'none' });
            reject(data);
          }
        } else if (res.statusCode === 401) {
          // token过期，清除登录状态
          wx.removeStorageSync('token');
          wx.showToast({ title: '请重新登录', icon: 'none' });
          setTimeout(() => {
            wx.switchTab({ url: '/pages/index/index' });
          }, 1500);
          reject({ code: 401, message: '未登录' });
        } else {
          wx.showToast({ title: '网络错误', icon: 'none' });
          reject({ code: res.statusCode, message: '网络错误' });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({ title: '网络连接失败', icon: 'none' });
        reject({ code: -1, message: err.errMsg || '网络连接失败' });
      }
    });
  });
};

/**
 * GET请求
 */
const get = (url, data, options = {}) => {
  return request({ url, method: 'GET', data, ...options });
};

/**
 * POST请求
 */
const post = (url, data, options = {}) => {
  return request({ url, method: 'POST', data, ...options });
};

/**
 * PUT请求
 */
const put = (url, data, options = {}) => {
  return request({ url, method: 'PUT', data, ...options });
};

/**
 * DELETE请求
 */
const del = (url, data, options = {}) => {
  return request({ url, method: 'DELETE', data, ...options });
};

module.exports = {
  request,
  get,
  post,
  put,
  del,
  getBaseUrl
};
