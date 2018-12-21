const hydrators = {
  downloadFile: (z, bundle) => {
    const filePromise = z.request({
      url: bundle.url,
      raw: true
    });
    return z.JSON.parse(filePromise.content) || {};
  },
};

module.exports = hydrators;
