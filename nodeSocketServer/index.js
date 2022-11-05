const express = require('express');
const app = express();
const http = require('http');
const server = http.createServer(app);
const { Server } = require('socket.io');
const io = new Server(server);

const jwt = require('jsonwebtoken');

const authSecret = process.env.JWT_SECRET;

const validIssuers = ["https://simplprint.azurewebsites.net/user/auth", "https://simplprint3d.com/user/auth"];



io.on('connection', (socket) =>{
    //Integrate JWT verification here
    const token = socket.handshake.auth.token;

    try {
        const decode = jwt.verify(token, authSecret);
        console.log(decode);
        if(decode.iss === null || !(decode.iss.includes(validIssuers[0]) || decode.iss.includes(validIssuers[1]))){
            console.log("invalid issuer");
            throw new Error("JWT token signed by invalid issuer - refusing connection");
        }
        username = decode.sub;
        roles = decode.roles;

        socket.emit("message","successfully authenticated");
        
        //Set up user specific rooms and send data via posts to clients you want to work with
    } catch (error) {
        console.log(error);

        socket.emit("error", "Invalid token provided");
        socket.disconnect();

    }
    

    console.log("new user connected: " + username);
    socket.on('disconnect', () => {
        console.log('user disconnected');
    })

});

server.listen(8080, () =>{
    console.log("listening on port 8080")
});