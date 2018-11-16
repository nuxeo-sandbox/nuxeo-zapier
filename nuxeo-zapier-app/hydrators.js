const hydrators = {
  downloadFile: (z, bundle) => {
    // use standard auth to request the file
    const filePromise = z.request({
      url: `http://zapier.apps.prod.nuxeo.io/nuxeo/nxfile/default/2fc54f59-1ced-4093-9575-a889b9ae128d`,
      raw: true
    });
    // and swap it for a stashed URL
    return z.stashFile(filePromise)
      .then((url) => {
        z.console.log(`Stashed URL = ${url}`);
        return url;
      });
  },
};

module.exports = hydrators;
