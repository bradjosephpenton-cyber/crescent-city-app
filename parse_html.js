const fs = require('fs');
const html = fs.readFileSync('output.html', 'utf8');

// Use regex to find any URLs that are audio or streams or contain songid
const regex = /(https?:\/\/[^\s"'<>]+)/g;
let match;
const urls = new Set();
while ((match = regex.exec(html)) !== null) {
    const url = match[1];
    if (url.includes('song') || url.includes('mp3') || url.includes('stream') || url.includes('audio') || url.includes('track') || url.includes('play')) {
        urls.add(url);
    }
}

console.log('Found URLs:');
for (const url of urls) {
    console.log(url);
}
