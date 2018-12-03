/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Nuxeo
 */
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
