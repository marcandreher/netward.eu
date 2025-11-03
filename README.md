# Netward - Intelligent Reverse Proxy with DNS Management

Netward is a high-performance service designed to protect website owners by hiding IP addresses and accelerating requests through modern technology. The platform includes an authoritative DNS service, allowing you to host your own nameservers and manage your domains seamlessly.

## 📦 Components

- **Netward Proxy** - Java 21 / Vert.x reverse proxy with extra features
- **PowerDNS** - Authoritative DNS server
- **PowerDNS Admin** - Web-based DNS management
- **MariaDB 11** - Database backend
- **phpMyAdmin** - Database administration tool

## 🔒 Security Features

- Direct IP access blocking
- Host validation against authorized zones
- Request/response header sanitization
- X-Real-IP and X-Forwarded-For headers
- Configurable connection timeouts

## 🚀 Performance

- **Async/Non-blocking I/O** - Powered by Vert.x event loop
- **Connection Pooling** - HikariCP for database, configurable HTTP client pool
- **DNS Zone Caching** - 10-minute cache reduces database queries
- **Smart Cache Eviction** - LRU-based with size limits
- **Keep-Alive Connections** - Persistent connections to upstreams

## 📝 License

This project is licensed under the MIT License.

## 👤 Author

**Marc Andreher**
- GitHub: [@marcandreher](https://github.com/marcandreher)

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!