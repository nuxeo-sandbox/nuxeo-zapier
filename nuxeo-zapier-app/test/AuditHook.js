require('should');

const zapier = require('zapier-platform-core');

const App = require('../index');
const appTester = zapier.createAppTester(App);

describe('triggers', () => {

  describe('new audit trigger', () => {
    it('should load audit event from fake hook', (done) => {
      const bundle = {
        inputData: {
          event: 'documentCreated'
        },
        cleanedRequest: {
          id: 1,
          docPath: '/bliblou',
        }
      };
      appTester(App.triggers.AuditHook.operation.perform, bundle)
        .then(results => {
          results.length.should.eql(1);
          const firstRecipe = results[0];
          firstRecipe.id.should.eql('1');
          firstRecipe.docPath.should.eql('/bliblou');

          done();
        })
        .catch(done);
    });
  });
});
