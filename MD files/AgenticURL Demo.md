PROJECT DEMO:

Single-view home page:  
![](../specs/screenshots/ss1.png)

Add URL manually for short link:  
![](../specs/screenshots/ss2.png)

Analytics: (To view link stats and total URLs list)  
![](../specs/screenshots/ss3.png)

![](../specs/screenshots/ss4.png)

Agentic Orchestration: (Human-in-loop for approvals before submitting)

STEP 1: Submit URL Shorten based on Prompt based on Greenfield\\Brownfield\\Ambiguous   
![](../specs/screenshots/ss5.png)

STEP 2: Check the Metrics to view the Health check.  
![](../specs/screenshots/ss6.png)

STEP 3: Intentionally stop the Greenfield node before submitting URL request to check full managed orchestration for resiliency.  
![](../specs/screenshots/ss7.png)

STEP 4: Make use of examples and update the URL\\Alias\\TTL. Submit the prompt while greenfield is down.  
![](../specs/screenshots/ss8.png)

STEP 5: Request pending for approval  
![](../specs/screenshots/ss9.png)

STEP 6: Though request approved but it is still waiting for the Greenfield service to come up\! in-progress bar is loading.

![](../specs/screenshots/ss10.png)

STEP 7: Request waiting in queue till Greenfield recovers in Orchestration without failure  
![](../specs/screenshots/ss11.png)

STEP 8: Greenfield node is healthy, and the request got completed after 8 retries.  
![](../specs/screenshots/ss12.png)

STEP 9: Fully Autonomous managed request submission without any approvals. (Beta \- still in progress)  
![](../specs/screenshots/ss13.png)

STEP 10: SWAGGER APIs for the URL services  
![](../specs/screenshots/ss14.png)

STEP 11: Fully resilient deployed service in Docker  
![](../specs/screenshots/ss15.png)
