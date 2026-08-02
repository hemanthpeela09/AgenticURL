**Setting environment: (LOCAL \+ PROD \+ LLM)**

**LOCAL SETUP:**  
Ui command:  (Project root folder)

1) cd ui  
2) npm install (if error install node js)  
3) Npm run build  
4) Npm run dev

URL \- [http://localhost:5174/](http://localhost:5174/)  
Application \- [http://localhost:8080/swagger-ui/index.html\#/](http://localhost:8080/swagger-ui/index.html#/)  
Health \- [http://localhost:8080/health](http://localhost:8080/health)

![](../specs/screenshots/st1.png)
![](../specs/screenshots/st2.png)
![](../specs/screenshots/st3.png)

**PROD STEP:**  
Docker command:

1) docker \-v (If error install docker in the local)  
2) docker compose build

![](../specs/screenshots/st4.png)

3) docker compose up \-d

![](../specs/screenshots/st5.png)

Greenfield URL \- [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)  
Brownfield URL \- [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)  
Frontend UI \- [http://localhost:4173](http://localhost:4173/)  

![](../specs/screenshots/st6.png)

**AI Mode \- LLM Support (Enable Ollama to run it locally free of cost)**

1) Go to file \- LlmConfig.java  
2) Changes these properties

![](../specs/screenshots/st7.png)

3) Check the model installed locally \- ollama list (In command prompt)

Model \- gemma4:12b  
![](../specs/screenshots/st8.png)

4) Change the “ollama” remove quotes

![](../specs/screenshots/st9.png)

5) Comment OpenAI **line 54** and uncomment Ollama **line 60**, in build.gradle

![](../specs/screenshots/st9.png)

6) Update the application properties

![](../specs/screenshots/st10.png)

7) Run the spring boot application and NPM from first page.

![](../specs/screenshots/st11.png)
	

8) Load the application and check the model.   
   

![](../specs/screenshots/st12.png)
