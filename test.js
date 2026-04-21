const { Client } = require('pg');
const client = new Client({ connectionString: 'postgresql://finsight:finsight@localhost:5435/finsight_user' });
client.connect().then(() => client.query('SELECT * FROM notifications LIMIT 1').then(res => { console.log(res.rows[0]); client.end(); }));
