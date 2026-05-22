const fs = require('fs');
const https = require('https');
const urlModule = require('url');

const initialUrl = 'https://www.soundclick.com/artist/default.cfm?bandID=1337361';

function fetchUrl(targetUrl, depth = 0) {
    if (depth > 5) {
        console.log('Too many redirects');
        return;
    }
    console.log(`Fetching: ${targetUrl}`);
    const parsedUrl = urlModule.parse(targetUrl);
    const options = {
        hostname: parsedUrl.hostname,
        path: parsedUrl.path,
        port: parsedUrl.port || (parsedUrl.protocol === 'https:' ? 443 : 80),
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
            'Accept-Language': 'en-US,en;q=0.9',
            'Cache-Control': 'no-cache',
            'Pragma': 'no-cache'
        }
    };

    https.get(options, (res) => {
        console.log(`Status: ${res.statusCode}`);
        if (res.headers.location) {
            console.log(`Redirecting to: ${res.headers.location}`);
            let nextUrl = res.headers.location;
            if (!nextUrl.startsWith('http')) {
                nextUrl = parsedUrl.protocol + '//' + parsedUrl.hostname + nextUrl;
            }
            fetchUrl(nextUrl, depth + 1);
            return;
        }

        let data = '';
        res.on('data', (chunk) => {
            data += chunk;
        });
        res.on('end', () => {
            fs.writeFileSync('output.html', data);
            console.log('Successfully written, size:', data.length);
        });
    }).on('error', (err) => {
        console.error('Error:', err.message);
    });
}

fetchUrl(initialUrl);
