/* Example */
'use strict';
const should = require('should');

const zapier = require('zapier-platform-core');

const App = require('../index');
const appTester = zapier.createAppTester(App);

describe('repo trigger', () => {
  zapier.tools.env.inject();

  it('should get a project', (done) => {
    const bundle = {
      authData: {
        username: process.env.CPXN_TEST_USERNAME,
        password: process.env.CPXN_TEST_PASSWORD
      },
      inputData: {
        filter: 'all'
      }
    };
    appTester(App.triggers.repo.operation.perform, bundle)
      .then((response) => {
        response.should.be.an.instanceOf(Array);
        done();
      })
      .catch(done);
  });
});
