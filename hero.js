var system = require('system');
var page = require('webpage').create();

var fs = require('fs');


page.onConsoleMessage = function(msg, lineNum, sourceId) {
	console.log('CONSOLE: ' + msg);
	if (!fs.exists(system.args[1] + '.log')) {
	  fs.write(system.args[1] + '.log', msg + '\r\n', 'a');
	}else {
		fs.touch(system.args[1] + '.log');
		fs.write(system.args[1] + '.log', msg + '\r\n', 'a');
	}
	
};

page.onLoadFinished = function() {
console.log(system.args[1]);

	//phantom.addCookie({'name' : 'user', 'value': system.args[1], 'domain' : '.heroeswm.ru'});
	//phantom.addCookie({'name' : 'pass', 'value': system.args[2], 'domain' : '.heroeswm.ru'});
	//phantom.addCookie({'name' : 'action', 'value': system.args[3], 'domain' : '.heroeswm.ru'});
	page.injectJs('jquery.min.js');
	page.injectJs('resources/public/js/heroes.js');
		
};
	
page.onLoadStarted = function() {
    console.log("page.onLoadStarted");
};


page.onUrlChanged = function() {
    console.log("page.onUrlChanged");
};
page.settings.userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:31.0) Gecko/20100101 Firefox/31.0";

page.open('http://www.heroeswm.ru/', function(status) {

	page.evaluate(function(system){
        	localStorage.setItem("user", system.args[1]);
		localStorage.setItem("pass", system.args[2]);
    	}, system);
	
});


