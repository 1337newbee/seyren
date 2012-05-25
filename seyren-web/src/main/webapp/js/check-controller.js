/*global console,$ */

function CheckController() {
    this.$xhr.defaults.headers.post['Content-Type'] = 'application/json';
    this.$xhr.defaults.headers.put['Content-Type'] = 'application/json';
    this.id = this.$route.current.params.id;
    
    this.alertStartIndex = 0;
    this.alertItemsPerPage = 10;
    
    this.pollAlertsInSeconds = 5;
    this.secondsToUpdateAlerts = this.pollAlertsInSeconds;
    this.$defer(this.countdownToRefreshAlerts, 1000);
    
    this.loadConfig();
}

CheckController.prototype = {
    
    loadConfig : function () {
        this.$xhr('GET', this.seyrenBaseUrl + '/api/config', this.loadConfigSuccess, this.loadConfigFailure);
    },
    
    loadConfigSuccess : function (code, response) {
        this.config = response;
    },
    
    loadConfigFailure : function (code, response) {
        console.log('Loading config failed');
    },
        
    loadCheck : function () {
        this.$xhr('GET', this.seyrenBaseUrl + '/api/checks/' + this.id, this.loadCheckSuccess, this.loadCheckFailure);
    },
    
    loadCheckSuccess : function (code, response) {
        this.check = response;
    },
    
    loadCheckFailure : function (code, response) {
        console.log('Loading check failed');
    },
    
    loadAlerts : function () {
        this.$xhr('GET', this.seyrenBaseUrl + '/api/checks/' + this.id + '/alerts?start=' + this.alertStartIndex + '&items=' + this.alertItemsPerPage, this.loadAlertsSuccess, this.loadAlertsFailure);
    },
    
    loadAlertsSuccess : function (code, response) {
        this.alerts = response;
    },
    
    loadAlertsFailure : function (code, response) {
        console.log('Loading alerts failed');
    },
    
    deleteCheck : function () {
        this.$xhr('DELETE', this.seyrenBaseUrl + '/api/checks/' + this.id, this.deleteCheckSuccess, this.deleteCheckFailure);
    },
    
    deleteCheckSuccess : function (code, response) {
        $("#confirmCheckDeleteModal").modal("hide"); 
        this.$location.updateHash('/checks');
    },
    
    deleteCheckFailure : function (code, response) {
        console.log('Deleting check failed');
    },
    
    saveCheck : function () {
        this.$xhr('PUT', this.seyrenBaseUrl + '/api/checks/' + this.id, this.check, this.saveCheckSuccess, this.saveCheckFailure);
    },
    
    saveCheckSuccess : function (code, response) {
        this.loadCheck();
    },
    
    saveCheckFailure : function (code, response) {
        console.log('Saving check failed');
    },
    
    createSubscription : function () {
        var subscription = {
            target : this.newsubscription.target,
            type : this.newsubscription.type,
            su : this.newsubscription.su,
            mo : this.newsubscription.mo,
            tu : this.newsubscription.tu,
            we : this.newsubscription.we,
            th : this.newsubscription.th,
            fr : this.newsubscription.fr,
            sa : this.newsubscription.sa,
            fromTime : this.newsubscription.fromTime,
            toTime : this.newsubscription.toTime,
            enabled : this.newsubscription.enabled
        };
        
        this.$xhr('POST', this.seyrenBaseUrl + '/api/checks/' + this.id + '/subscriptions', subscription, this.createSubscriptionSuccess, this.createSubscriptionFailure);
    },
    
    createSubscriptionSuccess : function (code, response) {
        $("#addSubscriptionModal").modal("hide"); 
        this.newsubscription.target = '';
        this.loadCheck();
    },
    
    createSubscriptionFailure : function (code, response) {
        console.log('Creating subscription failed');
    },
    
    swapEnabled : function (subscription) {
        subscription.enabled = !subscription.enabled;
        this.updateSubscription(subscription);
    },
    
    updateSubscription : function (subscription) {
        this.$xhr('PUT', this.seyrenBaseUrl + '/api/checks/' + this.id + '/subscriptions/' + subscription.id, subscription, this.updateSubscriptionSuccess, this.updateSubscriptionFailure);
    },
    
    updateSubscriptionSuccess : function (code, response) {
        this.loadCheck();
    },
    
    updateSubscriptionFailure : function (code, response) {
        console.log('Updating subscription failed');
    },
    
    deleteSubscription : function (subscriptionId) {
        this.$xhr('DELETE', this.seyrenBaseUrl + '/api/checks/' + this.id + '/subscriptions/' + subscriptionId, this.deleteSubscriptionSuccess, this.deleteSubscriptionFailure);
    },
    
    deleteSubscriptionSuccess : function (code, response) {
        this.loadCheck();
    },
    
    deleteSubscriptionFailure : function (code, response) {
        console.log('Deleting subscription failed');
    },
    
    loadOlderAlerts : function () {
        if (this.alerts.values.length !== this.alertItemsPerPage) {
            return;
        }
        this.alertStartIndex += this.alertItemsPerPage;
        this.loadAlerts();
    },
    
    loadNewerAlerts : function () {
        if (this.alertStartIndex === 0) {
            return;
        }
        this.alertStartIndex -= this.alertItemsPerPage;
        this.loadAlerts();
    },
    
    countdownToRefreshAlerts : function() {
        this.secondsToUpdateAlerts--;
        if (this.secondsToUpdateAlerts <= 0) {
            this.secondsToUpdateAlerts = this.pollAlertsInSeconds;
            this.loadAlerts();
        } 
        this.$defer(this.countdownToRefreshAlerts, 1000);
    },
    
    getSmallGraphUrl : function(minutes) {
        if (this.config && this.check) {
            var result = this.config.graphite.baseUrl + '/render/?hideLegend=true&width=365&height=70&hideAxes=true';
            result += '&target=alias(dashed(color(constantLine(' + this.check.warn + '),"yellow")),"warn level")';
            result += '&target=dashed(color(constantLine(' + this.check.error + '),"red"))';
            result += '&from=' + minutes + 'Minutes';
            result += '&target=' + this.check.target;
            return result;
        }
    },
    
    getBigGraphUrl : function(minutes) {
        if (this.config && this.check) {
            var result = this.config.graphite.baseUrl + '/render/?width=1400&height=350';
            result += '&target=alias(dashed(color(constantLine(' + this.check.warn + '),"yellow")),"warn level")';
            result += '&target=alias(dashed(color(constantLine(' + this.check.error + '),"red")),"error level")';
            result += '&from=' + minutes + 'Minutes';
            result += '&target=' + this.check.target;
            return result;
        }
    }
    
};
