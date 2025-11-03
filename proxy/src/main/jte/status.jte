@param String host
@param String message
@param String requestId
@param int status
@param String ipAddress
@param String statusFull
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${statusFull} - ${host}</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Segoe UI Symbol', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: #ffffff;
            color: #172b4d;
            line-height: 1.6;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        .header {
            padding: 20px 40px;
            border-bottom: 1px solid #e3e8ee;
        }

        .logo {
            font-size: 24px;
            font-weight: 600;
            color: #0051c3;
        }

        .main-content {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 40px 20px;
        }

        .error-container {
            max-width: 600px;
            width: 100%;
        }

        .error-code {
            font-size: 20px;
            font-weight: 600;
            color: #0051c3;
            margin-bottom: 16px;
        }

        h1 {
            font-size: 40px;
            font-weight: 600;
            color: #172b4d;
            margin-bottom: 16px;
            line-height: 1.2;
        }

        .description {
            font-size: 18px;
            color: #4a5568;
            margin-bottom: 32px;
        }

        .info-box {
            background: #f9fafb;
            border: 1px solid #e3e8ee;
            border-radius: 8px;
            padding: 24px;
            margin-bottom: 32px;
        }

        .info-title {
            font-size: 16px;
            font-weight: 600;
            color: #172b4d;
            margin-bottom: 16px;
        }

        .info-grid {
            display: grid;
            gap: 12px;
        }

        .info-item {
            display: flex;
            font-size: 14px;
        }

        .info-label {
            color: #6b7280;
            min-width: 120px;
            font-weight: 500;
        }

        .info-value {
            color: #172b4d;
            font-family: 'SF Mono', Monaco, 'Courier New', monospace;
            word-break: break-all;
        }

        .suggestions {
            background: #eff6ff;
            border-left: 4px solid #0051c3;
            padding: 20px 24px;
            margin-bottom: 32px;
            border-radius: 4px;
        }

        .suggestions-title {
            font-size: 16px;
            font-weight: 600;
            color: #172b4d;
            margin-bottom: 12px;
        }

        .suggestions ul {
            margin-left: 20px;
            color: #4a5568;
        }

        .suggestions li {
            margin-bottom: 8px;
            font-size: 14px;
        }

        .additional-info {
            font-size: 13px;
            color: #6b7280;
            margin-top: 40px;
            padding-top: 24px;
            border-top: 1px solid #e3e8ee;
        }

        .additional-info a {
            color: #0051c3;
            text-decoration: none;
        }

        .additional-info a:hover {
            text-decoration: underline;
        }

        .footer {
            padding: 32px 40px;
            border-top: 1px solid #e3e8ee;
            text-align: center;
            font-size: 13px;
            color: #6b7280;
        }

        .footer-links {
            display: flex;
            justify-content: center;
            gap: 24px;
            margin-bottom: 16px;
        }

        .footer-links a {
            color: #0051c3;
            text-decoration: none;
        }

        .footer-links a:hover {
            text-decoration: underline;
        }

        @media (max-width: 768px) {
            .header {
                padding: 16px 20px;
            }

            h1 {
                font-size: 32px;
            }

            .info-item {
                flex-direction: column;
                gap: 4px;
            }

            .info-label {
                min-width: auto;
            }

            .footer {
                padding: 24px 20px;
            }

            .footer-links {
                flex-direction: column;
                gap: 12px;
            }
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">NetWard</div>
    </div>

    <div class="main-content">
        <div class="error-container">
            <div class="error-code">Error ${status}</div>

            <h1>${statusFull}</h1>

            <p class="description">
                ${message}
            </p>

            <div class="info-box">
                <div class="info-title">Error details</div>
                <div class="info-grid">
                    <div class="info-item">
                        <span class="info-label">Ray ID:</span>
                        <span class="info-value">${requestId}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Your IP address:</span>
                        <span class="info-value">${ipAddress}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Timestamp:</span>
                        <span class="info-value" id="timestamp"></span>
                    </div>
                </div>
            </div>

            <div class="suggestions">
                <div class="suggestions-title">What can I do?</div>
                <ul>
                    <li>Try again in a few minutes</li>
                    <li>Check if the website is down for everyone or just you</li>
                    <li>Clear your browser cache and cookies</li>
                    <li>Contact the website administrator if the issue persists</li>
                </ul>
            </div>

            <%-- <div class="additional-info">
                <strong>Additional troubleshooting information</strong><br>
                A 502 Bad Gateway error means that the server received an invalid response from an upstream server. This is typically a temporary issue that resolves itself. If you're the site owner, check your origin server's error logs for more details.
            </div> --%>
        </div>
    </div>

    <div class="footer">
        <div class="footer-links">
            <a href="#">Support Portal</a>
            <a href="#">Status Page</a>
            <a href="#">Documentation</a>
        </div>
        <div>NetWard.eu &copy; 2025</div>
    </div>

    <script>
        const now = new Date();
        const timestamp = now.toISOString().replace('T', ' ').split('.')[0] + ' UTC';
        document.getElementById('timestamp').textContent = timestamp;
    </script>
</body>
</html>