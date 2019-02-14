( function($) {
    $(document).ready(
        function(){

            var _accept = readCookie("_accept_cookies");
            if (_accept !== undefined && _accept != null)
                return;

            div = $("<div class='cookie-consent'><p>This site uses cookies for anonymous web statistics. <a href='/privacy' target='_blank'>Find out more on how we use cookies and how you can opt-out</a>.</p></div>");

            button_accept = $("<button type='button' class='btn btn-xs btn-success'>I accept</button>").click(accept_cookie);
            button_deny = $("<button type='button' class='btn btn-xs btn-danger'>I refuse cookies</button>").click(deny_cookie);

            div.find('p').append(button_accept).append(button_deny);
            $("body").prepend(div);

        }
    );

    function accept_cookie() {
        createCookie("_accept_cookies", true, 365);
        $(".cookie-consent").remove();
    }

    function deny_cookie() {
        createCookie("_accept_cookies", false, 365);
        $(".cookie-consent").remove();
    }

    function createCookie(name,value,days) {
        var date,
            expires;

        if (days) {
            date = new Date();
            date.setTime(date.getTime()+(days*24*60*60*1000));
            expires = "; expires="+date.toGMTString();
        } else {
            expires = "";
        }
        document.cookie = name+"="+escape(value)+expires+"; path=/;";
    }

    function readCookie(name) {
        var nameEQ = name + "=",
            ca = document.cookie.split(';'),
            i,
            c;

        for(i=0;i < ca.length;i=i+1) {
            c = ca[i];
            while (c.charAt(0) === ' ') {
                c = c.substring(1,c.length);
            }
            if (c.indexOf(nameEQ) === 0) {
                return unescape(c.substring(nameEQ.length,c.length));
            }
        }
        return null;
    }

}) ( jQuery );
