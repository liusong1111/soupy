//TODO: use class Widget instead
function Widget(){

}

$(document).ready(function(){
    $('.draggable').draggable();
    $('.droppable').droppable({
        drop: function(event, ui){
            $(this).append(ui);
        }
    });

});
