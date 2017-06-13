var market = 'all';
var obApp = angular.module('statusApp', ['ngStomp']); 
obApp.controller('statusController', function ($stomp, $scope) {
	$stomp.connect('https://localhost/marketdata-websocket', {})
		.then(function (frame) {
			var subscription = $stomp.subscribe('/topic/exchangeStatus', 
				function (payload, headers, res) {
						$scope.exchanges = payload;
						$scope.$apply($scope.exchanges);
                });
            $stomp.send("/app/exchangeStatus", market);
     });
});