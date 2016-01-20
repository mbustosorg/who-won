/*

    Copyright (C) 2016 Mauricio Bustos (m@bustos.org)

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
  $('#addRider').on('click', handleAddRider);
  $('#updateRider').on('click', handleUpdateRider);
  $('#deleteRider').on('click', handleDeleteRider);
  $('#retrieveRider').on('click', handleRetrieveRider);

  function handleAddRider() {
    var bibNumber = $('#inputBibNumber').val();
    var name = $('#inputName').val();
    if (bibNumber && name) {
      $.ajax({
        type: "POST",
        url: "/rider/" + bibNumber,
        contentType: "application/x-www-form-urlencoded; charset=utf-8",
        data: encodeURI("name=" + name),
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
        },
        success: function(data){
           if (data.bibNumber == 0) {
              $('#authenticationAlert').addClass('alert-danger');
              $('#authenticationAlert').removeClass('alert-success');
              $('#authenticationAlert').html('<strong>   Error: </strong> Could not create rider');
           } else {
              $('#authenticationAlert').addClass('alert-success');
              $('#authenticationAlert').removeClass('alert-danger');
              $('#authenticationAlert').html('<strong>   Success: </strong> Rider created');
           }
        }
      });
    }
    return false;
  }

  function handleUpdateRider() {
    var bibNumber = $('#inputBibNumber').val();
    var name = $('#inputName').val();
    if (bibNumber && name) {
      $.ajax({
        type: "POST",
        url: "/rider/" + bibNumber + "/update",
        contentType: "application/x-www-form-urlencoded; charset=utf-8",
        data: encodeURI("name=" + name),
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
        },
        success: function(data){
           if (data.bibNumber == 0) {
              $('#authenticationAlert').addClass('alert-danger');
              $('#authenticationAlert').removeClass('alert-success');
              $('#authenticationAlert').html('<strong>   Error: </strong> Could not update rider');
           } else {
              $('#authenticationAlert').addClass('alert-success');
              $('#authenticationAlert').removeClass('alert-danger');
              $('#authenticationAlert').html('<strong>   Success: </strong> Rider updated');
           }
        }
      });
    }
    return false;
  };

  function handleDeleteRider() {
    var bibNumber = $('#inputBibNumber').val();
    if (isNaN(bibNumber) || !bibNumber) {
      $('#authenticationAlert').addClass('alert-danger');
      $('#authenticationAlert').removeClass('alert-success');
      $('#authenticationAlert').html('<strong>   Error: </strong> Bib number is not numeric');
    } else {
      $.ajax({
        type: "POST",
        url: "/rider/" + bibNumber + "/delete",
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
        },
        success: function(data){
           $('#inputName').val('');
           $('#inputBibNumber').val('');
           $('#authenticationAlert').addClass('alert-success');
           $('#authenticationAlert').removeClass('alert-danger');
           $('#authenticationAlert').html('<strong>   Success: </strong> Bib number deleted');
        }
      });
    }
    return false;
  }

  function handleRetrieveRider() {
    var bibNumber = $('#inputBibNumber').val();
    if (isNaN(bibNumber) || !bibNumber) {
      $('#authenticationAlert').addClass('alert-danger');
      $('#authenticationAlert').removeClass('alert-success');
      $('#authenticationAlert').html('<strong>   Error: </strong> Bib number is not numeric');
    } else {
      $.ajax({
        type: "GET",
        url: "/rider/" + bibNumber,
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').addClass('alert-danger');
          $('#authenticationAlert').removeClass('alert-success');
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
        },
        success: function(data){
           if (data.bibNumber == 0) {
              $('#authenticationAlert').addClass('alert-danger');
              $('#authenticationAlert').removeClass('alert-success');
              $('#authenticationAlert').html('<strong>   Error: </strong> Bib number not found');
           } else {
              $('#inputName').val(data.name);
              $('#authenticationAlert').addClass('alert-success');
              $('#authenticationAlert').removeClass('alert-danger');
              $('#authenticationAlert').html('<strong>   Success: </strong> Bib number retrieved');
           }
        }
      });
    }
    return false;
  }

});