/// <reference path="assert.d.ts" />
/// <reference path="mocha.d.ts" />
/// <reference path="react-addons-test-utils.d.ts" />
import assert = require('assert');
import Navigation = require('../../Navigation/src/Navigation');
import NavigationReact = require('../src/NavigationReact');
import React = require('react');
import ReactTestUtils = require('react-addons-test-utils');

describe('NavigationLinkTest', function () {
    describe('<>', function () {
        it('should work', function(){
            var stateNavigator = new Navigation.StateNavigator([
                { key: 's', route: '' }
            ]);
            var renderer = ReactTestUtils.createRenderer();
            renderer.render(
                <NavigationReact.NavigationLink
                    stateKey="s"
                    stateNavigator={stateNavigator} />
            );
            var link = renderer.getRenderOutput();
            stateNavigator.historyManager.getUrl = () => link.props['href'].substring(1);
            link['ref']({ href: 'xxx' });
            link.props['onClick']({ preventDefault: () => {} });
            assert.equal(link.props['href'], '#/');
            assert.equal(stateNavigator.stateContext.state.key, 's');
        })
    });  
});
