const hydrators = require('../hydrators');

const spikeBlob = (z, bundle) => {
    let file = {};
    file.file = z.dehydrateFile(hydrators.downloadFile, {
      fileId: "blabla",
    });
    let files = [];
    file.id = "blabla";
    files.push(file);
    return files;
};

module.exports = {
  key: 'spikeBlob',
  noun: 'spikeBlob',

  display: {
    label: 'Spike Blob',
    description: 'Receiving a blob from Nuxeo',
  },

  operation: {
    perform: spikeBlob,
  },
};
