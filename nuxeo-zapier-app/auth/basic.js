'use strict';

const basic = {
  type: 'basic',
  test: {
    url: 'http://zapier.apps.prod.nuxeo.io/nuxeo/json/cmis',
  },
  connectionLabel: '- Connected with {{bundle.authData.username}}',
  fields:
    [
      {
        key: 'url',
        required: true,
        type: 'string',
      },
    ],
};
module.exports = basic;
