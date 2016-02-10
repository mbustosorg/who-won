/*

    Copyright (C) 2015 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

$(document).ready(function() {
  $("form#loginForm").submit(function() {
    var username = $('#inputName').val().toLowerCase().trim();
    var password = $('#inputPassword').val();
    $("#loggingIn").removeClass('hide');
    if (username && password) {
      var http = location.protocol;
	  var slashes = http.concat("//");
	  var host = slashes.concat(window.location.host);
      $.ajax({
        type: "POST",
        url: "/login",
        contentType: "application/x-www-form-urlencoded; charset=utf-8",
        data: "inputName=" + username + "&inputPassword=" + password,
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
          $('#authenticationAlert').removeClass('hide');
	      $("#loggingIn").addClass('hide');
        },
        success: function(data){
          if (data.error) {
          }
          else {
            window.location.replace(host.concat(data));
            window.location.reload();
          }
	      $("#loggingIn").addClass('hide');
        }
      });
    }
    return false;
  });

});