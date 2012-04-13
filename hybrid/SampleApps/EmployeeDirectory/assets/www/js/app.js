"use strict";

// Structure is updated after authentication
var sfdc = {
	INSTANCE: '',
	OAUTH_TOKEN: ''
};

// Creating the application namespace
var directory = {
    models: {},
    views: {},
    utils: {}
};

// -------------------------------------------------- Utilities ---------------------------------------------------- //

// The Template Loader. Used to asynchronously load templates located in separate .html files
directory.utils.templateLoader = {

    templates: {},

    load: function(names, callback) {

        var deferreds = [],
            self = this;

        $.each(names, function(index, name) {
            // deferreds.push($.get(SFHybridApp.buildLocalUrl('tpl/' + name + '.html'), function(data) {
        	deferreds.push($.get('tpl/' + name + '.html', function(data) {
                self.templates[name] = data;
            }));
        });

        $.when.apply(null, deferreds).done(callback);
    },

    // Get template by name from hash of preloaded templates
    get: function(name) {
        return this.templates[name];
    }

};

// -------------------------------------------------- The Models ---------------------------------------------------- //

// The Employee Model
directory.models.Employee = Backbone.Model.extend({

    url: function() {
        return  sfdc.INSTANCE + '/services/data/v24.0/chatter/users/' + this.id;
    },

    parse: function(resp, xhr) {
        var phoneNumbersMap = {};
        for (var phoneNumber in resp.phoneNumbers) {
            phoneNumbersMap[phoneNumber.type] = phoneNumber.number;
        }
        var city = '';
        if (resp.address && resp.address.city) city = resp.address.city;

        return {
            id: resp.id,
            firstName: resp.firstName,
            lastName: resp.lastName,
            title: resp.title || '',
            city: city,
            managerName: resp.managerName || '',
            managerId: resp.managerId || '',
            followersCount: resp.followersCount,
            officePhone: phoneNumbersMap['Work'] || '',
            cellPhone: phoneNumbersMap['Mobile'] || '',
            email: resp.email,
            photoUrl: resp.photo.smallPhotoUrl
        };
    },

    initialize: function() {
        this.followers = new directory.models.EmployeeCollection();
        this.followers.followingId = this.id;
    }

});

// The EmployeeCollection Model
directory.models.EmployeeCollection = Backbone.Collection.extend({

    model: directory.models.Employee,

    fetch: function() {
        var url = sfdc.INSTANCE + "/services/data/v24.0/chatter/users/" + this.followingId + "/followers";
        var self = this;
        $.ajax({
            url:url,
            dataType:"json",
            success:function (data) {
                console.log("followers success: " + data.followers.length);
                var subscribers = [];
                for (var i = 0; i < data.followers.length; i++) {
                    subscribers.push(data.followers[i].subscriber);
                }
                self.reset(subscribers, {'parse':true});
            }
        });
    },

    findByName: function(key) {
        var url = sfdc.INSTANCE + "/services/data/v24.0/chatter/users?q=" + encodeURIComponent(key);
        var self = this;
        $.ajax({
            url:url,
            dataType:"json",
            success:function (data) {
                console.log("search success: " + data.users.length);
                self.reset(data.users, {'parse':true});
            }
        });
    }

});


// -------------------------------------------------- The Views ---------------------------------------------------- //

directory.views.SearchPage = Backbone.View.extend({

    initialize: function() {
        this.template = _.template(directory.utils.templateLoader.get('search-page'));
    },

    render: function(eventName) {
        $(this.el).html(this.template(this.model.toJSON()));
        this.listView = new directory.views.EmployeeListView({el: $('ul', this.el), model: this.model});
        this.listView.render();
        return this;
    },

    events: {
        "keyup .search-key": "search"
    },

    search: function(event) {
        var key = $('.search-key').val();
        this.model.findByName(key);
    }
});

directory.views.FollowersPage = Backbone.View.extend({

    initialize: function() {
        this.template = _.template(directory.utils.templateLoader.get('report-page'));
    },

    render: function(eventName) {
        $(this.el).html(this.template(this.model.toJSON()));
        this.listView = new directory.views.EmployeeListView({el: $('ul', this.el), model: this.model});
        this.listView.render();
        return this;
    }

});

directory.views.EmployeeListView = Backbone.View.extend({

    initialize: function() {
        this.model.bind("reset", this.render, this);
    },

    render: function(eventName) {
        $(this.el).empty();
        _.each(this.model.models, function(employee) {
            $(this.el).append(new directory.views.EmployeeListItemView({model: employee}).render().el);
        }, this);
        return this;
    }

});

directory.views.EmployeeListItemView = Backbone.View.extend({

    tagName: "li",

    initialize: function() {
        this.template = _.template(directory.utils.templateLoader.get('employee-list-item'));
    },

    render: function(eventName) {
        $(this.el).html(this.template(this.model.toJSON()));
        return this;
    }

});

directory.views.EmployeePage = Backbone.View.extend({

    initialize: function() {
        this.template = _.template(directory.utils.templateLoader.get('employee-page'));
    },

    render: function(eventName) {
        $(this.el).html(this.template(this.model.toJSON()));
        return this;
    }

});

// ----------------------------------------------- The Application Router ------------------------------------------ //

directory.Router = Backbone.Router.extend({

    routes: {
        "": "list",
        "list": "list",
        "employees/:id": "employeeDetails",
        "employees/:id/followers": "followers"
    },

    initialize: function() {

        var self = this;

        // Keep track of the history of pages (we only store the page URL). Used to identify the direction
        // (left or right) of the sliding transition between pages.
        this.pageHistory = [];

        // Register event listener for back button troughout the app
        $('#content').on('click', '.header-back-button', function(event) {
            window.history.back();
            return false;
        });

        // Check of browser supports touch events...
        if (document.documentElement.hasOwnProperty('ontouchstart')) {
            // ... if yes: register touch event listener to change the "selected" state of the item
            $('#content').on('touchstart', 'a', function(event) {
                self.selectItem(event);
            });
            $('#content').on('touchend', 'a', function(event) {
                self.deselectItem(event);
            });
        } else {
            // ... if not: register mouse events instead
            $('#content').on('mousedown', 'a', function(event) {
                self.selectItem(event);
            });
            $('#content').on('mouseup', 'a', function(event) {
                self.deselectItem(event);
            });
        }

        // We keep a single instance of the SearchPage and its associated Employee collection throughout the app
        this.searchResults = new directory.models.EmployeeCollection();
        this.searchPage = new directory.views.SearchPage({model: this.searchResults});
        this.searchPage.render();
        $(this.searchPage.el).attr('id', 'searchPage');
    },

    selectItem: function(event) {
        $(event.target).addClass('tappable-active');
    },

    deselectItem: function(event) {
        $(event.target).removeClass('tappable-active');
    },

    list: function() {
        var self = this;
        this.slidePage(this.searchPage);
    },

    employeeDetails: function(id) {
        var employee = new directory.models.Employee({id: id}),
            self = this;
        employee.fetch({
            success: function(data) {
                self.slidePage(new directory.views.EmployeePage({model: data}).render());
            }
        });
    },

    followers: function(id) {
        var employee = new directory.models.Employee({id: id});
        employee.followers.fetch();
        this.slidePage(new directory.views.FollowersPage({model: employee.followers}).render());
    },

    slidePage: function(page) {

        var slideFrom,
            self = this;

        if (!this.currentPage) {
            // If there is no current page (app just started) -> No transition: Position new page in the view port
            $(page.el).attr('class', 'page stage-center');
            $('#content').append(page.el);
            this.pageHistory = [window.location.hash];
            this.currentPage = page;
            return;
        }

        // Cleaning up: remove old pages that were moved out of the viewport
        $('.stage-right, .stage-left').not('#searchPage').remove();

        if (page === this.searchPage) {
            // Always apply a Back (slide from left) transition when we go back to the search page
            slideFrom = "left";
            $(page.el).attr('class', 'page stage-left');
            // Reinitialize page history
            this.pageHistory = [window.location.hash];
        } else if (this.pageHistory.length > 1 && window.location.hash === this.pageHistory[this.pageHistory.length - 2]) {
            // The new page is the same as the previous page -> Back transition
            slideFrom = "left";
            $(page.el).attr('class', 'page stage-left');
            this.pageHistory.pop();
        } else {
            // Forward transition (slide from right)
            slideFrom = "right";
            $(page.el).attr('class', 'page stage-right');
            this.pageHistory.push(window.location.hash);
        }

        $('#content').append(page.el);

        // Wait until the new page has been added to the DOM...
        setTimeout(function() {
            // Slide out the current page: If new page slides from the right -> slide current page to the left, and vice versa
            $(self.currentPage.el).attr('class', 'page transition ' + (slideFrom === "right" ? 'stage-left' : 'stage-right'));
            // Slide in the new page
            $(page.el).attr('class', 'page stage-center transition');
            self.currentPage = page;
        });

    }

});

// Bootstrap the application
function employeeDirectoryStart() {
    directory.utils.templateLoader.load(['search-page', 'report-page', 'employee-page', 'employee-list-item'],
        function() {
            directory.app = new directory.Router();
            Backbone.history.start();
        });
}