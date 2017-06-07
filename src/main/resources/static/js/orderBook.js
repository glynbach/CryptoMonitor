var market = 'all';
var obApp = angular.module('obApp', ['ngStomp']); 
obApp.controller('obController', function ($stomp, $scope) {
	$stomp.connect('https://localhost/marketdata-websocket', {})
		.then(function (frame) {
			var subscription = $stomp.subscribe('/topic/orderBooks', 
				function (payload, headers, res) {
						$scope.orderBooks = payload;
						$scope.$apply($scope.orderBooks);
                });
            $stomp.send("/app/orderBook", market);
     });
});