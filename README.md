# Findex

# 

> 가볍고 빠른 외부 Open API 연동 금융 분석 도구
> 
> 
> 💹 한눈에 보는 금융 지수 데이터! 외부 Open API를 통해 최신 지수 데이터를 자동으로 수집·분석·시각화합니다.

<img width="1068" height="443" alt="image" src="https://github.com/user-attachments/assets/d8e5a8e4-39c3-4670-b5ce-83318390276e" />

## 📌 소개

Findex는 외부 Open API와 연동하여 금융 지수 데이터를 제공하는 **대시보드 서비스**입니다.

사용자는 직관적인 UI로 지수 흐름을 파악하고, **자동 연동** 기능으로 최신 데이터를 분석할 수 있습니다.

지수별 **성과 분석**, **이동평균선** 계산, **자동 데이터 업데이트**를 통해 가볍고도 강력한 분석 환경을 제공합니다. 📈📊


- ## ✨ 주요 기능

### 1) Open API 연동

- 공공데이터포털 등 **외부 Open API** 연동 준비/구성 (인증키/요청 파라미터/쿼터 관리)

### 2) 지수 정보 관리

- 지수 정보(메타) 조회
- 지수 정보 **등록 / 수정 / 삭제**
- 지수 정보 **목록 조회**

### 3) 지수 데이터 관리 (담당)

- 지수 데이터(시계열) 조회
- 지수 데이터 **등록 / 수정 / 삭제**
- 지수 데이터 **지수, 기간별 목록 조회**
  - 특정 기준으로 정렬
- 지수 데이터 **Export** (CSV 등)

### 4) 연동 작업 관리

- 연동 **작업 정보** 조회
- **지수 정보 연동** (메타 싱크)
- **지수 데이터 연동** (시계열 싱크)
- **연동 작업 목록** 조회

### 5) 자동 연동 설정 관리

- 자동 연동 **정보/상태** 조회
- 자동 연동 설정 **등록 / 수정 / 목록 조회**
- **배치(스케줄러)에 의한 자동 갱신**

### 6) 대시보드

- **주요 지수 현황 요약**
- **지수 차트** (이동평균선 등 보조지표)
- **지수 성과 분석 랭킹**

---

## 🧱 기술 스택

- JAVA/SpringBoot
- JPA/QueryDSL
- PostgreSQL
- Git, GitHub

---

## 📁 프로젝트 구조

### ERD

<img width="1740" height="882" alt="image" src="https://github.com/user-attachments/assets/6aab5ab7-3366-4c29-9017-1845a346cf27" />

### UI

- 대시보드
  <img width="1456" height="1326" alt="image" src="https://github.com/user-attachments/assets/2648aaf8-bb73-460e-9216-61e7e88c0fba" />

- 지수 관리
  <img width="1465" height="978" alt="image" src="https://github.com/user-attachments/assets/94cd161b-f4cd-4815-b363-f8fd63b5907f" />

- 데이터 관리
  <img width="1466" height="961" alt="image" src="https://github.com/user-attachments/assets/de13fa25-f4ce-4f30-a8fe-e50c9f639ad4" />

- 연동 관리
  <img width="1464" height="1242" alt="image" src="https://github.com/user-attachments/assets/053891e7-7902-41d9-b954-4d18c80f1749" />


