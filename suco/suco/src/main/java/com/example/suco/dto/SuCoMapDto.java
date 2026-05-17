package com.example.suco.dto;

public class SuCoMapDto {

    private Long id;
    private Double viDo;
    private Double kinhDo;
    private String moTa;
    private String tenLoai;
    private String trangThaiDuyet;
    private String trangThaiXuLy;
    private String iconUrl;
    private String mucDoNghiemTrong;
    private String hinhAnhUrl;
    private Integer doTinCay; // QUAN TRỌNG: Thêm biến này
    private String tenNguoiBao;
    private TruSoMapDto truSoDeXuat;
    private TruSoMapDto truSoTiepNhan;
    private String tenDuong;
    private String quan;
    private String huyenHoac;
    private String thanhPho;
    private String diaChi;
    private String reporterUid;

    // Constructor rút gọn (thường dùng cho Socket nhanh)
    public SuCoMapDto(Long id, Double viDo, Double kinhDo, String moTa, String tenLoai, 
                      String trangThaiDuyet, String trangThaiXuLy, String iconUrl, 
                      String mucDoNghiemTrong, String hinhAnhUrl, Integer doTinCay, String tenNguoiBao) {
        this.id = id;
        this.viDo = viDo;
        this.kinhDo = kinhDo;
        this.moTa = moTa;
        this.tenLoai = tenLoai;
        this.trangThaiDuyet = trangThaiDuyet;
        this.trangThaiXuLy = trangThaiXuLy;
        this.iconUrl = iconUrl;
        this.mucDoNghiemTrong = mucDoNghiemTrong;
        this.hinhAnhUrl = hinhAnhUrl;
        this.doTinCay = doTinCay;
        this.tenNguoiBao = tenNguoiBao;
    }

    // Constructor đầy đủ địa chỉ
    public SuCoMapDto(Long id, Double viDo, Double kinhDo, String moTa, String tenLoai, String trangThaiDuyet,
                      String trangThaiXuLy, String iconUrl, String mucDoNghiemTrong, String hinhAnhUrl,
                      Integer doTinCay, String tenDuong, String quan, String huyenHoac, String thanhPho, 
                      String diaChi, String tenNguoiBao) {
        this(id, viDo, kinhDo, moTa, tenLoai, trangThaiDuyet, trangThaiXuLy, iconUrl, mucDoNghiemTrong, hinhAnhUrl, doTinCay, tenNguoiBao);
        this.tenDuong = tenDuong;
        this.quan = quan;
        this.huyenHoac = huyenHoac;
        this.thanhPho = thanhPho;
        this.diaChi = diaChi;
    }
    public SuCoMapDto(Long id,Double viDo,Double kinhDo,String moTa,String tenLoai,String trangThaiDuyet,
    String trangThaiXuLy,String iconUrl,String mucDoNghiemTrong,String hinhAnhUrl,Integer doTinCay,
    String tenDuong,String quan,String huyenHoac,String thanhPho,String diaChi,String tenNguoiBao,String reporterUid
) {
    this(id,viDo,kinhDo,moTa,tenLoai,trangThaiDuyet,trangThaiXuLy,iconUrl,mucDoNghiemTrong,hinhAnhUrl,doTinCay,
        tenDuong,quan,huyenHoac,thanhPho,diaChi,tenNguoiBao
    );
    this.reporterUid = reporterUid;
}

    // GETTERS (Bắt buộc phải có đủ để Jackson render JSON)
    public Long getId() { return id; }
    public Double getViDo() { return viDo; }
    public Double getKinhDo() { return kinhDo; }
    public String getMoTa() { return moTa; }
    public String getTenLoai() { return tenLoai; }
    public String getTrangThaiDuyet() { return trangThaiDuyet; }
    public String getTrangThaiXuLy() { return trangThaiXuLy; }
    public String getIconUrl() { return iconUrl; }
    public String getMucDoNghiemTrong() { return mucDoNghiemTrong; }
    public String getHinhAnhUrl() { return hinhAnhUrl; }
    public Integer getDoTinCay() { return doTinCay; } // Thêm getter
    public String getTenNguoiBao() { return tenNguoiBao; }
    public TruSoMapDto getTruSoDeXuat() { return truSoDeXuat; }
    public TruSoMapDto getTruSoTiepNhan() { return truSoTiepNhan; }
    public String getTenDuong() { return tenDuong; }
    public String getQuan() { return quan; }
    public String getHuyenHoac() { return huyenHoac; }
    public String getThanhPho() { return thanhPho; }
    public String getDiaChi() { return diaChi; }
    public String getReporterUid() { return reporterUid; }

    // SETTERS
    public void setDoTinCay(Integer doTinCay) { this.doTinCay = doTinCay; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public void setTenNguoiBao(String tenNguoiBao) { this.tenNguoiBao = tenNguoiBao; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public void setTruSoDeXuat(TruSoMapDto truSoDeXuat) { this.truSoDeXuat = truSoDeXuat; }
    public void setTruSoTiepNhan(TruSoMapDto truSoTiepNhan) { this.truSoTiepNhan = truSoTiepNhan; }
    public void setTenDuong(String tenDuong) { this.tenDuong = tenDuong; }
    public void setQuan(String quan) { this.quan = quan; }
    public void setHuyenHoac(String huyenHoac) { this.huyenHoac = huyenHoac; }
    public void setThanhPho(String thanhPho) { this.thanhPho = thanhPho; }
    public void setReporterUid(String reporterUid) { this.reporterUid = reporterUid; }

    

}