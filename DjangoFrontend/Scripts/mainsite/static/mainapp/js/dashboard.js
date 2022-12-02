document.getElementById("option_files").addEventListener("click", fileCallback)
document.getElementById("option_printers").addEventListener("click", printerCallback)
document.getElementById("add_file_button").addEventListener("click", addFileCallback)
document.getElementById("delete_file_button").addEventListener("click", deleteFileCallback)


var queuePrintButton = document.getElementById("queue_print")
var fileList;
var printerList;

if(queuePrintButton !== undefined){
    queuePrintButton.addEventListener("click", queuePrintCallback)
}


function fileCallback(){
    document.getElementById("printer_display").style.display = "none";
    document.getElementById("file_display").style.display = "block";
}

function printerCallback(){
    document.getElementById("printer_display").style.display = "block";
    document.getElementById("file_display").style.display = "none";
}

function queuePrintCallback(){
    console.log("queuePrint hit")
    // var displayBox = document.getElementById("container")
    var displayBox = document.getElementById("printer_display")
    
    var fileRequest = new XMLHttpRequest()

    var selectItems = document.createElement("SELECT");
    selectItems.className = "printer_files"
    selectItems.id = "printer_file_select"

    fileRequest.onreadystatechange = function(){
        if(this.readyState == 4 && this.status == 200){
            console.log(this.responseText)
            console.log(this.response)
            jsonRes = JSON.parse(this.responseText)
            console.log(jsonRes)
            if(jsonRes.message.includes("failed")){
                if(document.getElementById("error_message") === undefined){

                    var h2Error = document.createElement("H2");
                    
                    h2Error.innerHTML = "Had issues fetching the list of files for your account, please check that you have uploaded files for use!";

                    h2Error.className = "error_message";

                    displayBox.appendChild(h2Error)
                }else{
                    var h2Error = document.getElementById("error_message")

                    h2Error.style.display = "block"

                    h2Error.innerHTML = "Had issues fetching the list of files for your account, please check that you have uploaded files for use!";

                }

            }else{

                // if(document.getElementById("error_message") !== undefined){
                //     document.getElementById("error_message").style.display = "none"
                // }

                fileList = jsonRes.files;
                console.log(fileList)
                for(var j = 0; j < fileList.length; j++){
                    var option = document.createElement("OPTION")
                    option.id = fileList[j]
                    option.setAttribute("value", fileList[j])
                    option.innerHTML = fileList[j]
                    selectItems.appendChild(option)
                }
            }
        }
    }

    fileRequest.open("GET", "/getUserFileList/");
    fileRequest.send();

    printerRequest = new XMLHttpRequest();

    printerRequest.onreadystatechange = function(){
        if(this.readyState == 4 && this.status == 200){
            console.log(this.responseText);
            console.log(this.response);
            printerJsonRes = JSON.parse(this.responseText);
            console.log(printerJsonRes);
            if(printerJsonRes.message.includes("failed")){

                if(document.getElementById("error_message") === undefined){

                    var h2Error = document.createElement("H2");
                    
                    h2Error.innerHTML = "Had issues fetching your registered printers for your account, please check your printers!";

                    h2Error.className = "error_message";

                    displayBox.appendChild(h2Error)
                }else{
                    var h2Error = document.getElementById("error_message")

                    h2Error.style.display = "block"

                    h2Error.innerHTML = "Had issues fetching your registered printers for your account, please check your printers!";

                }
            }else{
                printerList = printerJsonRes.printers;
                console.log(printerList)
                if(document.getElementById("printer_box_container")){
                    document.getElementById("printer_box_container").remove()
                }
                var containerDiv = document.createElement("DIV")

                containerDiv.id = "printer_box_container"

                for(var i = 0; i < printerList.length; i++){
                    var printerDiv = document.createElement("DIV");
                    printerDiv.className = "printer_box";
                    printerDiv.id = printerList[i];
            
                    //set up a stock image for use
                    var printerName = document.createElement("P");
            
                    var printerButton = document.createElement("DIV");

                    printerButton.innerHTML = "Start print";

                    printerButton.className = "button";

                    printerButton.addEventListener("click", function(e){
                        // e = e || window.event;
                        
                        console.log("queue print clicked")
                        var target = e.target;
                        var parent = target.parentElement
                        var select = parent.querySelector("select")
                        var selectedValue = select.value
                        // var selectedText = select.options[select.selectedIndex].text

                        console.log(selectedValue)
                        // console.log(selectedText)
                        console.log(parent.id)


                        var jsonBodyToSend = {
                            "printer": parent.id,
                            "file": selectedValue
                        }

                        queuePrintRequest = new XMLHttpRequest()
                        
                        queuePrintRequest.onreadystatechange = function(){
                            if(this.readyState == 4 && this.status == 200){
                                console.log(this.responseText)
                                console.log(this.response)

                                var parsedResponse = JSON.parse(this.responseText);

                                console.log(parsedResponse)

                                if(parsedResponse.message.includes("failed")){
                                    if(document.getElementById("error_message") === undefined){

                                        var h2Error = document.createElement("H2");
                                        
                                        h2Error.innerHTML = "Had issues queuing a print, please try re-logging in and trying again.";

                                        h2Error.className = "error_message";

                                        displayBox.appendChild(h2Error)
                                    }else{
                                        var h2Error = document.getElementById("error_message")

                                        h2Error.style.display = "block"

                                        h2Error.innerHTML = "Had issues queuing a print, please try re-logging in and trying again.";

                                    }
                                }
                                
                            }
                        }

                        queuePrintRequest.open("POST", "/queuePrint/")
                        queuePrintRequest.setRequestHeader("Content-Type", "application/json")
                        queuePrintRequest.send(JSON.stringify(jsonBodyToSend))

                        var ackMessage = document.createElement("P")
                        ackMessage.innerHTML = "Print queued successfully!"
                        displayBox.append(ackMessage);
                    })
                    printerName.innerHTML = printerList[i];
                    // selectItems.id = "printer_file_select" + "_" + printerList[i];
                    var cloneSelectItems = selectItems.cloneNode(true)
                    printerDiv.appendChild(printerName);
                    printerDiv.appendChild(cloneSelectItems);
                    printerDiv.appendChild(printerButton);
                    containerDiv.appendChild(printerDiv);
                }
                displayBox.appendChild(containerDiv)

            }
        }
    }

    printerRequest.open("GET", "/getUserPrinters/");
    printerRequest.send();
    

}

function addFileCallback(){
    console.log("add file hit")
    var fileDisplayBox = document.getElementById("file_display")
    var fileInput = document.createElement("INPUT")
    fileInput.type = 'file';

    fileInput.onchange = e => {
        var file = e.target.files[0]
    
        console.log(file.name)
        console.log(file.size)
        console.log(file.type)
    
        data = {
            "file": file,
        }

        var addFileRequest = new XMLHttpRequest();
    
        addFileRequest.onreadystatechange = function(){
            if(this.readyState == 4 && this.status == 200){
                console.log(this.responseText)
                var jsonResponse = JSON.parse(this.responseText)
                if(!(jsonResponse.message.includes("failed"))){

                    var fileAddParagraph = document.createElement("P");
                    fileAddParagraph.innerHTML = "Your file has been uploaded.";
                    fileAddParagraph.class = "file_added"
                    fileDisplayBox.appendChild(fileAddParagraph)
                }else{

                    var h2Error = document.createElement("H2");
                                        
                    h2Error.innerHTML = "Had issues adding a file, please try logging back in and trying again.";

                    h2Error.className = "error_message";

                    displayBox.appendChild(h2Error)

                }
            }
    
        }
    
        addFileRequest.open("POST", "/addFile/")
        addFileRequest.send(file)
        
    }
    
    fileDisplayBox.appendChild(fileInput)
    // fileDisplayBox.insertBefore(document.getElementById("add_file_button"), fileInput)
}

function deleteFileCallback(){
    console.log("delete file hit")

    var displayBox = document.getElementById("file_display");

    var fileRequest = new XMLHttpRequest()

    var selectItems = document.createElement("SELECT");
    selectItems.className = "printer_files"
    selectItems.id = "printer_file_select"

    fileRequest.onreadystatechange = function(){
        if(this.readyState == 4 && this.status == 200){
            console.log(this.responseText)
            console.log(this.response)
            jsonRes = JSON.parse(this.responseText)
            console.log(jsonRes)
            if(jsonRes.message.includes("failed")){
                if(!document.getElementById("error_message")){

                    var h2Error = document.createElement("H2");
                    
                    h2Error.innerHTML = "Had issues fetching the list of files for your account, please check that you have uploaded files for use!";

                    h2Error.className = "error_message";

                    displayBox.appendChild(h2Error)
                }else{
                    var h2Error = document.getElementById("error_message")

                    h2Error.style.display = "block"

                    h2Error.innerHTML = "Had issues fetching the list of files for your account, please check that you have uploaded files for use!";

                }

            }else{

                // if(document.getElementById("error_message") !== undefined){
                //     document.getElementById("error_message").style.display = "none"
                // }

                fileList = jsonRes.files;
                console.log(fileList)
                for(var j = 0; j < fileList.length; j++){
                    var option = document.createElement("OPTION")
                    option.id = fileList[j]
                    option.setAttribute("value", fileList[j])
                    option.innerHTML = fileList[j]
                    selectItems.appendChild(option)
                }
            }
        }
    }

    fileRequest.open("GET", "/getUserFileList/");
    fileRequest.send();

    if(document.getElementById("printer_box_container")){
        document.getElementById("printer_box_container").remove()
    }
    var containerDiv = document.createElement("DIV")

    containerDiv.id = "printer_box_container"

    var printerDiv = document.createElement("DIV");
    printerDiv.className = "printer_box";
    // printerDiv.id = printerList[i];

    //set up a stock image for use
    // var printerName = document.createElement("P");

    var printerButton = document.createElement("DIV");

    printerButton.innerHTML = "Delete file";

    printerButton.className = "button";

    printerButton.addEventListener("click", function(e){
        // e = e || window.event;
        
        console.log("Delete file clicked")
        var target = e.target;
        var parent = target.parentElement
        var select = parent.querySelector("select")
        var selectedValue = select.value
        // var selectedText = select.options[select.selectedIndex].text

        console.log(selectedValue)
        // console.log(selectedText)
        // console.log(parent.id)


        var jsonBodyToSend = {
            // "printer": parent.id,
            "filename": selectedValue
        }

        queuePrintRequest = new XMLHttpRequest()
        
        queuePrintRequest.onreadystatechange = function(){
            if(this.readyState == 4 && this.status == 200){
                console.log(this.responseText)
                console.log(this.response)

                var parsedResponse = JSON.parse(this.responseText);

                console.log(parsedResponse)

                if(parsedResponse.message.includes("failed")){
                    if(!document.getElementById("error_message")){

                        var h2Error = document.createElement("H2");
                        
                        h2Error.innerHTML = "Had issues deleting a file, please try re-logging in and trying again.";

                        h2Error.className = "error_message";

                        displayBox.appendChild(h2Error)
                    }else{
                        var h2Error = document.getElementById("error_message")

                        h2Error.style.display = "block"

                        h2Error.innerHTML = "Had issues deleting a file, please try re-logging in and trying again.";

                    }
                }
                
            }
        }

        queuePrintRequest.open("DELETE", "/deleteFile/")
        queuePrintRequest.setRequestHeader("Content-Type", "application/json")
        queuePrintRequest.send(JSON.stringify(jsonBodyToSend))

        var ackMessage = document.createElement("P")
        ackMessage.innerHTML = "File deleted successfully - refresh for updates"
        displayBox.append(ackMessage);
    })
    // printerName.innerHTML = printerList[i];

    // printerDiv.appendChild(printerName);
    printerDiv.appendChild(selectItems);
    printerDiv.appendChild(printerButton);
    containerDiv.appendChild(printerDiv);
    displayBox.appendChild(containerDiv)
}







