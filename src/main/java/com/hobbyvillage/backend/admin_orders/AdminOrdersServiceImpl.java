package com.hobbyvillage.backend.admin_orders;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@EnableScheduling
@Service
public class AdminOrdersServiceImpl implements AdminOrdersService {

	private AdminOrdersMapper mapper;

	@Value("${import-base-url}")
	private String import_baseURL;

	@Value("${api-key}")
	private String api_Key;

	@Value("${api-secret-key}")
	private String api_Secret;

	@Value("${sweettracker-base-url}")
	private String sweettracker_baseURL;

	@Value("${sweettracker-api-key}")
	private String sweettracker_api_Key;

	public AdminOrdersServiceImpl(AdminOrdersMapper mapper) {
		this.mapper = mapper;
	}

	private String filtering(String filter) {
		if (filter.equals("none")) {
			filter = "odrState IS NOT NULL";

		} else if (filter.equals("payment-completed")) {
			filter = "odrState = '결제 완료'";

		} else if (filter.equals("preparing-for-delivery")) {
			filter = "odrState = '배송 준비 중'";

		} else if (filter.equals("shipping")) {
			filter = "odrState = '배송 중'";

		} else if (filter.equals("delivery-completed")) {
			filter = "odrState = '배송 완료'";

		} else if (filter.equals("returning")) {
			filter = "odrState = '반납 중'";

		} else if (filter.equals("returned")) {
			filter = "odrState = '반납 완료'";

		} else if (filter.equals("cancel-request")) {
			filter = "odrState = '취소 요청'";

		} else {
			filter = "odrState = '취소 처리 완료'";
		}

		return filter;
	}

	@Override
	public String getImportToken() throws IOException {
		// 요청 URL 설정
		URL url = new URL(import_baseURL + "/users/getToken");

		// 요청(통신) 객체 생성
		HttpsURLConnection con = null;

		// 요청 URL, 요청 방식, 요청 데이터 타입(이곳 -> 아임포트), 응답 데이터 타입(아임포트 -> 이곳)
		con = (HttpsURLConnection) url.openConnection(); // 요청 URL 설정
		con.setRequestMethod("POST"); // 요청 방식 설정
		con.setRequestProperty("Content-type", "application/json"); // 요청 데이터 타입(이곳 -> 아임포트) 설정
		con.setRequestProperty("Accept", "application/json"); // 응답 데이터 타입(아임포트 -> 이곳) 설정
		con.setDoOutput(true); // POST 방식으로 BODY에 데이터를 담아 보내도록 설정

		// BODY에 담을 JSON 형식의 데이터를 만들기 위한 JSON 객체 생성
		JsonObject json = new JsonObject();

		// JSON 객체에 값 입력
		json.addProperty("imp_key", api_Key);
		json.addProperty("imp_secret", api_Secret);

		// 데이터 전송 준비
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
		// 전송할 데이터를 버퍼에 저장
		bw.write(json.toString());
		// 데이터 전송
		bw.flush();
		// 데이터 전송 객체 종료
		bw.close();

		// 아임포트의 응답 데이터를 버퍼에 저장
		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));

		// 응답 데이터(JSON 형식)를 파싱하기 위한 GSON 객체 생성
		Gson gson = new Gson();

		// 버퍼에 저장된 응답 데이터를 Map 형식으로 불러온 뒤,
		// .get("response")로 key가 response인 value를 받아오고,
		// .toString()으로 해당 value를 String 타입으로 변환해 저장
		String response = gson.fromJson(br.readLine(), Map.class).get("response").toString();

		// 마찬가지의 과정으로 response 내에서 key가 access_token인 value를 받아와 String 타입으로 변환해 저장
		String token = gson.fromJson(response, Map.class).get("access_token").toString();

		// 응답 데이터 객체 종료

		br.close();
		// 요청 객체 종료
		con.disconnect();

		return token;
	}

	@Override // 미검색 상태에서 주문 개수 조회
	public int getOrderCount(String sort, String filter) {
		filter = filtering(filter);
		int orderCount;

		// 반납기한 정렬 여부 확인(반납기한이 설정되지 않은 데이터 제외 처리)
		if (sort.equals("-deadline")) {
			orderCount = mapper.getDeliveriedOrderCount(filter);
		} else {
			orderCount = mapper.getAllOrderCount(filter);
		}

		return orderCount;
	}

	@Override // 검색 상태에서 주문 개수 조회
	public int getSearchOrderCount(String sort, String condition, String keyword, String filter) {
		filter = filtering(filter);
		int orderCount;

		// 반납기한 정렬 여부 확인(반납기한이 설정되지 않은 데이터 제외 처리)
		if (sort.equals("-deadline")) {
			orderCount = mapper.getSearchDeliveriedOrderCount(condition, keyword, filter);
		} else {
			orderCount = mapper.getSearchAllOrderCount(condition, keyword, filter);
		}

		return orderCount;
	}

	@Override // 미검색 상태에서 주문 목록 조회
	public List<AdminOrdersDTO> getOrderList(String sort, String filter, int pageNum) {
		filter = filtering(filter);
		List<AdminOrdersDTO> orderList;

		// 반납기한 정렬 여부 확인(반납기한이 설정되지 않은 데이터 제외 처리)
		if (sort.equals("-deadline")) {
			orderList = mapper.getDeliveriedOrderList(sort, filter, pageNum);
		} else {
			orderList = mapper.getAllOrderList(sort, filter, pageNum);
		}

		return orderList;
	}

	@Override // 검색 상태에서 주문 목록 조회
	public List<AdminOrdersDTO> getSearchOrderList(String condition, String keyword, String sort, String filter,
			int pageNum) {
		filter = filtering(filter);
		List<AdminOrdersDTO> orderList;

		// 반납기한 정렬 여부 확인(반납기한이 설정되지 않은 데이터 제외 처리)
		if (sort.equals("-deadline")) {
			orderList = mapper.getSearchDeliveriedOrderList(condition, keyword, sort, filter, pageNum);
		} else {
			orderList = mapper.getSearchAllOrderList(condition, keyword, sort, filter, pageNum);
		}

		return orderList;
	}

	@Override // 주문 상세 조회
	public AdminOrdersDetailDTO getOrderDetail(String odrNumber) {
		AdminOrdersDetailDTO result = mapper.getOrderDetail(odrNumber);
		int exactPrice = mapper.getOrderExactPrice(odrNumber);
		int usedSavedMoney = mapper.getOrderUsedSavedMoney(odrNumber);

		result.setExactPrice(exactPrice);
		result.setUsedSavedMoney(usedSavedMoney);

		return result;
	}

	@Override // 각 주문 별 상품 목록 조회
	public List<AdminOrdersProductsDTO> getOrderedProductList(String odrNumber) {
		return mapper.getOrderedProductList(odrNumber);
	}

	@Override // 주문자 주소 수정
	public int modifyOrderAddress(AdminOrdersDetailDTO addressData) {
		return mapper.modifyOrderAddress(addressData);
	}

	@Override // 주문 확인 처리 (결제 완료 -> 배송 준비 중)
	public int payCompToPreDeli(String odrNumber) {
		return mapper.payCompToPreDeli(odrNumber);
	}

	@Override // 운송 정보 등록 처리 (배송 준비 중 -> 배송 중)
	public int preDeliToShipping(int opCode, String courierCompany, String trackingNumber) {
		return mapper.preDeliToShipping(opCode, courierCompany, trackingNumber);
	}

	@Override // 주문 취소 처리 과정 1: 현재 주문 상태 확인
	public String checkOdrState(int opCode) {
		return mapper.checkOdrState(opCode);
	}

	@Override // 주문 취소 처리 과정 2: 취소 금액 및 환급 적립금 설정
	public int cancelOrder(AdminOrdersCancelOrderDTO data, String token) throws IOException {
		String odrNumber = data.getOdrNumber(), // 주문번호
				prodCode = data.getProdCode(), odrEmail = data.getOdrEmail(); // 주문자 이메일
		int opCode = data.getOpCode(), // 주문 상품 코드
				prodPrice = data.getProdPrice(), // 상품 단위 가격
				rentalPeriod = data.getRentalPeriod(), // 상품 대여 기간
				prodShipping = data.getProdShipping(), // 상품 배송비
				exactPrice = data.getExactPrice(), // 실제 주문 가격 (상품 최종 가격 - 사용 적립금)
				usedSavedMoney = data.getUsedSavedMoney(); // 사용 적립금
		int realPrice = (prodPrice * rentalPeriod / 7) + prodShipping; // 상품의 실제 총 가격
		int amount; // 아임포트 취소 금액
		int returnSavedMoney; // 환급 적립금

		int result; // 메서드 결과

		// 주문번호에 해당하는 상품의 총 개수 구하기
		String filter = "odrNumber = '" + odrNumber + "'";
		int allOrderProducts = mapper.getAllOrderCount(filter);

		// 주문 상품이 하나 뿐
		if (allOrderProducts == 1) {
			// 이 경우 exactPrice를 환불 금액으로 설정
			amount = exactPrice;
			returnSavedMoney = usedSavedMoney;
		}
		// 주문 상품이 둘 이상
		else {
			// 주문번호에 해당하는 상품들 중 취소 가능한 상품의 총 개수 구하기
			// (결제 완료, 배송 준비 중, 취소 요청)
			filter = "odrNumber = '" + odrNumber + "' AND odrState IN('결제 완료', '배송 준비 중', '취소 요청')";
			int cancelableProducts = mapper.getAllOrderCount(filter);

			// 해당 주문의 취소 금액과 사용 취소된 적립금을 조회
			AdminOrdersCancelPriceDTO canceledData = mapper.getCanceledData(odrNumber);
			int cancelPrice = canceledData.getCancelPrice();
			int cancelSavedMoney = canceledData.getCancelSavedMoney();

			// 주문 상품이 둘 이상이고, 환불 가능한 상품이 하나인 경우
			if (cancelableProducts == 1) {
				// 나머지 상품들이 모두 주문 취소된 상태인지 확인
				filter = "odrNumber = '" + odrNumber + "' AND odrState = '주문 취소 완료'";
				int canceledProducts = mapper.getAllOrderCount(filter);

				// 전체 상품 수 = 취소 가능 상품(1) + 취소된 상품인 경우
				if (allOrderProducts == cancelableProducts - canceledProducts) {
					// == 나머지 상품이 모두 취소된 상태에서 마지막 상품까지 취소하는 상황
					amount = exactPrice - cancelPrice;
					returnSavedMoney = usedSavedMoney - cancelSavedMoney;
				}
				// 배송 중 ~ 반납 완료 상태인 상품이 하나 이상 존재하는 상황(전액 환불 불가)
				else {
					if (exactPrice - cancelPrice <= 100) {
						// 최종 남은 주문 금액이 100원 이하인 경우, 아임포트 환불 없이 DB에서만 처리
						mapper.cnclReqTocnclComp(opCode); // '주문 취소 완료' 처리
						mapper.modifyRentalState(prodCode); // 상품 상태 미대여로 변경

						// 환급할 적립금 설정
						// 주문에 저장된 남은 적립금이 상품 가격이상인 경우, 환급 적립금 = 상품 가격
						if (usedSavedMoney - cancelSavedMoney >= realPrice) {
							returnSavedMoney = realPrice;
						}
						// 남은 적립금이 상품 가격 미만인 경우, 환급 적립금 = 남은 적립금
						else {
							returnSavedMoney = usedSavedMoney - cancelSavedMoney;
						}

						mapper.returnSavedMoney(odrEmail, returnSavedMoney); // 적립금 환급
						return mapper.cancelSavedMoneyChange(odrNumber, returnSavedMoney); // 주문 테이블 cancelSavedMoney 설정
					}
					// 최종 남은 주문 금액이 100원보다 많은 경우
					else {
						// 상품 금액이 남은 주문 금액 - 100 이하인 경우, amount = 상품 가격, 환급 적립금 = 0
						if (realPrice <= exactPrice - cancelPrice - 100) {
							amount = realPrice;
							returnSavedMoney = 0;
						}
						// 상품 금액이 남은 주문 금액 - 100 보다 큰 경우, amount = 남은 주문 금액 - 100
						else {
							amount = exactPrice - cancelPrice - 100;

							// 환급할 적립금 설정
							// 주문에 저장된 남은 적립금이 상품 가격이상인 경우, 환급 적립금 = 상품 가격
							if (usedSavedMoney - cancelSavedMoney >= realPrice) {
								returnSavedMoney = realPrice;
							}
							// 남은 적립금이 상품 가격 미만인 경우, 환급 적립금 = 남은 적립금
							else {
								returnSavedMoney = usedSavedMoney - cancelSavedMoney;
							}
						}
					}
				}
			}
			// 주문 상품이 둘 이상이고, 환불 가능한 상품이 둘 이상인 경우
			else {
				// 남은 주문 금액이 100원 이하인 경우
				if ((exactPrice - cancelPrice) <= 100) {
					// 남은 적립금이 0원이라면
					if ((usedSavedMoney - cancelSavedMoney) == 0) {
						mapper.cnclReqTocnclComp(opCode); // '주문 취소 완료' 처리
						mapper.modifyRentalState(prodCode); // 상품 상태 미대여로 변경
						return 1;
					}
					// 남은 적립금이 0원이 아니라면
					else {
						// 상품 금액이 남은 적립금이하인 경우, 환급 적립금 = 상품 금액
						if (realPrice <= (usedSavedMoney - cancelSavedMoney)) {
							returnSavedMoney = realPrice;
						}
						// 상품 금액이 남은 적립금보다 큰 경우, 남은 적립금 모두 환급
						else {
							returnSavedMoney = usedSavedMoney - cancelSavedMoney;
						}

						mapper.cnclReqTocnclComp(opCode); // '주문 취소 완료' 처리
						mapper.modifyRentalState(prodCode); // 상품 상태 미대여로 변경
						mapper.returnSavedMoney(odrEmail, returnSavedMoney); // 적립금 환급
						return mapper.cancelSavedMoneyChange(odrNumber, returnSavedMoney); // 주문 테이블 cancelSavedMoney 설정
					}
				}
				// 남은 주문 금액이 100원보다 많은 경우
				else {
					// 상품 금액이 남은 주문 금액 - 100 이하인 경우, amount = 상품 금액, 환급 적립금 = 0
					if (realPrice <= (exactPrice - cancelPrice - 100)) {
						amount = realPrice;
						returnSavedMoney = 0;
					}
					// 상품 금액이 남은 주문 금액 - 100 보다 큰 경우, amount = exactPrice - cancelPrice - 100
					else {
						amount = exactPrice - cancelPrice - 100;

						// 상품 금액 - amount 가 남은 적립금 이하라면,
						// returnSavedMoney = (prodPrice * rentalPeriod / 7) + prodShipping - amount
						if (realPrice - amount <= (usedSavedMoney - cancelSavedMoney)) {
							returnSavedMoney = realPrice - amount;
						}
						// 상품 금액이 남은 적립금보다 많다면 returnSavedMoney = usedSavedMoney - cancelSavedMoney
						else {
							returnSavedMoney = usedSavedMoney - cancelSavedMoney;
						}
					}
				}
			}
		}

		// 여기서 아임포트 호출
		String imp_uid = mapper.getImpUid(odrNumber);

		result = cancelOrder(imp_uid, token);

		if (result == 0) {
			mapper.cnclReqTocnclComp(opCode); // '주문 취소 완료' 처리
			mapper.modifyRentalState(prodCode); // 상품 상태 미대여로 변경
			mapper.returnSavedMoney(odrEmail, returnSavedMoney); // 적립금 환급
			mapper.cancelPriceChange(odrNumber, amount); // 주문 테이블 cancelPrice 설정
			mapper.cancelSavedMoneyChange(odrNumber, returnSavedMoney); // 주문 테이블 cancelSavedMoney 설정
			result = 1;
		}

		return result;
	}

	public int cancelOrder(String imp_uid, String token) throws IOException {
		URL url = new URL(import_baseURL + "/payments/cancel");

		HttpsURLConnection con = null;

		con = (HttpsURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Authorization", token);
		con.setDoOutput(true);

		JsonObject json = new JsonObject();

		json.addProperty("imp_uid", imp_uid);

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));

		bw.write(json.toString());
		bw.flush();
		bw.close();

		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));

		Gson gson = new Gson();

		int code = gson.fromJson(br.readLine(), ImportPaymentCheckDTO.class).getCode();

		System.out.println("캔슬 code : " + code);

		br.close();
		con.disconnect();

		return code;
	}

	@Override // 반납 완료 처리
	public int returningToReturned(int opCode, String prodCode, int prodPrice, int rentalPeriod, String email) {
		int result = 0;
		int savedMoney = (prodPrice * (rentalPeriod / 7)) * 5 / 100;
		String nextStep = "반납 완료";
		String odrState = mapper.checkOdrState(opCode);

		if (odrState.equals("반납 중")) {
			mapper.modifyOdrState(opCode, nextStep);
			mapper.modifyRentalState(prodCode);
			result = mapper.giveSavedMoney(email, savedMoney);
		}

		return result;
	}

	@Scheduled(fixedDelay = 4500000) // 1시간 15분마다 실행
	public void deliveryTracking() { // 상품 배송 상태 파악
		List<AdminOrdersTrackingDTO> trackingList = mapper.getTrackingData();

		for (AdminOrdersTrackingDTO trackingData : trackingList) {
			try {
				// true 가 출력되면 배송 완료
				boolean isComplete = trackingResult(trackingData);

				if (isComplete) {
					// 배송 중 -> 배송 완료 변경
					mapper.deliveryCompleted(trackingData.getOpCode());
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("테스트!!");
	}

	@Override
	public boolean trackingResult(AdminOrdersTrackingDTO trackingData) throws IOException {
		int opCode = trackingData.getOpCode();
		String courierCompany = trackingData.getCourierCompany();
		String trackingNumber = trackingData.getTrackingNumber();

		String trueURL = sweettracker_baseURL + "/api/v1/trackingInfo?t_key=" + sweettracker_api_Key;
		trueURL += "&t_code=";
		trueURL += courierCompany;
		trueURL += "&t_invoice=";
		trueURL += trackingNumber;

		URL url = new URL(trueURL);

		HttpURLConnection con = null;

		con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);

		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));

		Map<String, Object> response = new ObjectMapper().readValue(br.readLine(), Map.class);

		String code = (String) response.get("code");
		if (code != null) {
			return false;
		}

		Boolean complete = (Boolean) response.get("complete");

		br.close();
		con.disconnect();

		return complete;
	}

}
