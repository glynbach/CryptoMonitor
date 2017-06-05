var market = 'Poloniex';
var obApp = angular.module('obApp', ['ngStomp']); 
obApp.controller('obController', function ($stomp, $scope) {
	$stomp.connect('https://localhost/marketdata-websocket', {})
		.then(function (frame) {
			var subscription = $stomp.subscribe('/topic/orderBooks', 
				function (payload, headers, res) {
						$scope.orderBook = payload;
						//$scope.obEntries = []
						//angular.forEach($scope.orderBook.obEntries, function(obEntry){
						//	$scope.obEntries.push(obEntry);
						//	console.log(obEntry);  
						//})
						$scope.$apply($scope.orderBook);
						//$scope.$apply($scope.obEntries);
                });
            $stomp.send("/app/orderBook", market);
     });
});