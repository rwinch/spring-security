function Message(data) {
    this.id = ko.observable(data.id)
    this.text = ko.observable(data.text)
    this.summary = ko.observable(data.summary)
    this.created = ko.observable(data.created)
}

function MessageListViewModel() {
    var self = this;
    self.messages = ko.observableArray([]);
    self.chosenMessageData = ko.observable();
    self.inbox = ko.observable();
    self.compose = ko.observable();

    self.goToMessage = function(message) {
        self.inbox(null);
        $.getJSON("rest/" + message.id(), self.chosenMessageData);
    };

    self.goToCompose = function(data) {
        self.inbox(null);
        self.chosenMessageData(null);
        self.compose(new Message([]));
    };

    self.goToInbox = function() {
        $.getJSON("rest/", function(allData) {
            var mappedMessages = $.map(allData, function(item) { return new Message(item) });
            self.messages(mappedMessages);
            self.inbox(mappedMessages);
            self.chosenMessageData(null);
            self.compose(null);
        });
    }

    self.save = function() {
        $.ajax("rest/", {
            data: ko.toJSON(self.compose),
            type: "post", contentType: "application/json",
            success: function(result) {
                $.map(result, function(item) { return new Message(item) });
                self.goToInbox();
            }
        });
    };

    self.goToInbox();
}

$(function () {
  var token = $("meta[name='_csrf']").attr("content");
  var header = $("meta[name='_csrf_header']").attr("content");
  $(document).ajaxSend(function(e, xhr, options) {
    xhr.setRequestHeader( "Content-type", "application/json" );
    xhr.setRequestHeader(header, token);
  });
  ko.applyBindings(new MessageListViewModel())
});

