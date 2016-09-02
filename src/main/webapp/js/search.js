$("#search-box").keyup(function() {
        var search = $(this).val();
        $("#testId").show();
        if (search)
            $("#testId").not(":containsNoCase(" + search + ")").hide();
});

$.expr[":"].containsNoCase = function (el, i, m) {
    var search = m[3];
    if (!search) return false;
      return new RegExp(search,"i").test($(el).text());
};


// jQuery Plug-in example
$("#testId")
    .searchFilter({targetSelector: "#testId"})

