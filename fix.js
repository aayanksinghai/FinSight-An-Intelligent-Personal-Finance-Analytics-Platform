const fs = require('fs');
let content = fs.readFileSync('frontend/src/hooks/useStompClient.ts', 'utf8');
content = content.replace(/const title = [^;]+;/s, '');
content = content.replace(/toast\([\s\S]*?\);\n/s, '');
content = content.replace(/import toast from 'react-hot-toast';\n/, '');
fs.writeFileSync('frontend/src/hooks/useStompClient.ts', content);
