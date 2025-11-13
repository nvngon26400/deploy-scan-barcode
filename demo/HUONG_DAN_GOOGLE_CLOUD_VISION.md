# Hướng Dẫn Cấu Hình Google Cloud Vision API

## Tổng Quan
Google Cloud Vision API là một dịch vụ mạnh mẽ của Google để nhận diện và phân tích hình ảnh, đặc biệt tốt trong việc detect barcode từ ảnh mờ/chói.

## Bước 1: Tạo Project trên Google Cloud Platform

1. **Truy cập Google Cloud Console:**
   - Vào: https://console.cloud.google.com/
   - Đăng nhập bằng tài khoản Google của bạn

2. **Tạo Project mới:**
   - Click vào dropdown project ở thanh trên cùng (có thể hiển thị "Select a project")
   - Click "NEW PROJECT"
   - Điền thông tin:
     - **Project name**: `barcode-scanner` (hoặc tên bạn muốn)
     - **Organization**: Chọn organization nếu có (có thể bỏ qua)
     - **Location**: Chọn location phù hợp
   - Click "CREATE"
   - Đợi vài giây để project được tạo

3. **Chọn Project:**
   - Sau khi tạo xong, chọn project vừa tạo từ dropdown

## Bước 2: Enable Vision API

1. **Mở API Library:**
   - Vào menu ☰ (góc trên bên trái)
   - Chọn "APIs & Services" > "Library"

2. **Tìm Vision API:**
   - Tìm kiếm "Cloud Vision API" trong search box
   - Click vào "Cloud Vision API" từ kết quả

3. **Enable API:**
   - Click nút "ENABLE"
   - Đợi vài giây để API được kích hoạt

## Bước 3: Tạo API Key

### Cách 1: Tạo API Key (Đơn giản nhất - cho development)

1. **Vào Credentials:**
   - Menu ☰ > "APIs & Services" > "Credentials"

2. **Tạo API Key:**
   - Click "CREATE CREDENTIALS" ở trên cùng
   - Chọn "API key"
   - Một API key sẽ được tạo tự động

3. **Sao chép API Key:**
   - Copy API key (dạng: `AIzaSy...`)
   - **Lưu ý**: Giữ bí mật API key này!

4. **Giới hạn API Key (Khuyến nghị):**
   - Click vào API key vừa tạo để chỉnh sửa
   - Trong "API restrictions":
     - Chọn "Restrict key"
     - Chọn "Cloud Vision API"
   - Trong "Application restrictions":
     - Có thể chọn "HTTP referrers" và thêm domain của bạn
     - Hoặc "None" cho development
   - Click "SAVE"

### Cách 2: Tạo Service Account (Cho production)

1. **Tạo Service Account:**
   - Menu ☰ > "IAM & Admin" > "Service Accounts"
   - Click "CREATE SERVICE ACCOUNT"
   - Điền thông tin:
     - **Service account name**: `barcode-scanner-service`
     - **Service account ID**: Tự động tạo
   - Click "CREATE AND CONTINUE"

2. **Gán Role:**
   - Trong "Grant this service account access to project":
     - Chọn role: "Cloud Vision API User"
   - Click "CONTINUE" > "DONE"

3. **Tạo Key:**
   - Click vào service account vừa tạo
   - Tab "KEYS" > "ADD KEY" > "Create new key"
   - Chọn "JSON"
   - Click "CREATE"
   - File JSON sẽ được download về máy

4. **Sử dụng Service Account:**
   - Nếu dùng Service Account, cần thay đổi code để sử dụng JSON key file
   - Hoặc set biến môi trường `GOOGLE_APPLICATION_CREDENTIALS` trỏ đến file JSON

## Bước 4: Cấu Hình Trong Application

### Cách 1: Dùng API Key (Đơn giản)

1. **Mở file `application.properties`:**
   ```
   src/main/resources/application.properties
   ```

2. **Thêm API Key:**
   ```properties
   google.vision.api.key=AIzaSy... (paste API key của bạn vào đây)
   google.vision.api.url=https://vision.googleapis.com/v1/images:annotate
   ```

3. **Lưu file**

### Cách 2: Dùng Biến Môi Trường (Khuyến nghị cho production)

1. **Không hardcode API key trong file:**
   ```properties
   google.vision.api.key=${GOOGLE_VISION_API_KEY:}
   google.vision.api.url=https://vision.googleapis.com/v1/images:annotate
   ```

2. **Set biến môi trường:**
   - **Windows (PowerShell):**
     ```powershell
     $env:GOOGLE_VISION_API_KEY="AIzaSy..."
     ```
   - **Windows (CMD):**
     ```cmd
     set GOOGLE_VISION_API_KEY=AIzaSy...
     ```
   - **Linux/Mac:**
     ```bash
     export GOOGLE_VISION_API_KEY="AIzaSy..."
     ```

3. **Hoặc trong Docker:**
   ```dockerfile
   ENV GOOGLE_VISION_API_KEY=AIzaSy...
   ```

## Bước 5: Kiểm Tra Cấu Hình

1. **Khởi động ứng dụng:**
   ```bash
   ./gradlew bootRun
   ```

2. **Test API:**
   - Gửi request đến endpoint `/api/barcode/decode` với ảnh barcode
   - Nếu cấu hình đúng, bạn sẽ thấy log:
     ```
     Bước 2: Thử Google Cloud Vision API...
     ```

3. **Kiểm tra Logs:**
   - Nếu có lỗi, kiểm tra log để xem:
     - API key có hợp lệ không
     - Vision API đã được enable chưa
     - Có đủ quota không

## Bước 6: Quản Lý Billing (Quan Trọng!)

1. **Kiểm tra Billing:**
   - Google Cloud có free tier cho Vision API:
     - **1,000 requests/tháng miễn phí**
     - Sau đó: $1.50 cho mỗi 1,000 requests

2. **Thiết Lập Billing Alert:**
   - Menu ☰ > "Billing"
   - Chọn "Budgets & alerts"
   - Tạo budget để nhận cảnh báo khi chi phí vượt ngưỡng

3. **Giới Hạn Quota:**
   - Menu ☰ > "APIs & Services" > "Quotas"
   - Tìm "Cloud Vision API"
   - Có thể set giới hạn requests/ngày

## Troubleshooting

### Lỗi: "API key not valid"
- Kiểm tra API key đã copy đúng chưa
- Kiểm tra API key đã được restrict đúng API chưa
- Kiểm tra Vision API đã được enable chưa

### Lỗi: "Permission denied"
- Kiểm tra service account có đủ quyền không
- Kiểm tra API key có quyền truy cập Vision API không

### Lỗi: "Quota exceeded"
- Đã hết free tier (1,000 requests/tháng)
- Cần enable billing để tiếp tục sử dụng

### Lỗi: "API not enabled"
- Vào API Library và enable Cloud Vision API
- Đợi vài phút để API được kích hoạt hoàn toàn

## Bảo Mật

⚠️ **QUAN TRỌNG:**
- **KHÔNG** commit API key vào Git
- Thêm `application.properties` vào `.gitignore` nếu chứa key
- Sử dụng biến môi trường cho production
- Giới hạn API key theo domain/IP nếu có thể
- Rotate API key định kỳ

## Tài Liệu Tham Khảo

- [Google Cloud Vision API Documentation](https://cloud.google.com/vision/docs)
- [Vision API Pricing](https://cloud.google.com/vision/pricing)
- [API Key Best Practices](https://cloud.google.com/docs/authentication/api-keys)

